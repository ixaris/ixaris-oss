package com.ixaris.commons.zeromq.microservices;

import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.Collections;
import java.util.concurrent.CompletionStage;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleRequest;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.example.client.ExampleStub;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleSkeleton;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleSupportSkeletonImpl;
import com.ixaris.commons.zeromq.microservices.examplesupport.client.ExampleSupportStub;
import com.ixaris.commons.zeromq.microservices.stack.TestServiceSecurityChecker;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceChannel;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceClientSupport;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceSupport;

public class ZMQReconnectResilienceIT {
    
    private static final Logger LOG = LoggerFactory.getLogger(ZMQReconnectResilienceIT.class);
    
    private static final int START_PORT = 39000;
    
    private static MultiTenancy multiTenancy;
    private static TestZMQServiceChannel channel;
    
    private ExampleSupportSkeletonImpl exampleSupportSkeleton;
    
    @BeforeClass
    public static void setupClass() {
        ZMQGlobal.start();
        
        final TestServiceSecurityChecker testServiceSecurityChecker = new TestServiceSecurityChecker();
        final LocalService localService = new LocalService(Collections.emptySet(), Collections.emptySet());
        
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        
        channel = new TestZMQServiceChannel(new TestZMQServiceSupport(multiTenancy, START_PORT, localService, testServiceSecurityChecker, true),
            new TestZMQServiceClientSupport(multiTenancy, localService, true));
        
        multiTenancy.addTenant(TestTenants.DEFAULT);
    }
    
    @AfterClass
    public static void teardownClass() {
        multiTenancy.stop();
        ZMQGlobal.shutdown();
    }
    
    @Before
    public void setup() {}
    
    @Test
    public void servicesShouldReactToReconnections() {
        
        final ServiceSkeletonProxy<ExampleSkeleton> proxy = channel.serviceSupport.getOrCreate(ExampleSkeleton.class);
        final ExampleSkeleton.Watch exampleEventPublisher = proxy.createPublisherProxy(ExampleSkeleton.Watch.class);
        final ExampleSupportStub exampleSupportResource = channel.serviceClientSupport
            .getOrCreate(ExampleSupportStub.class)
            .createProxy();
        final ExampleResourceImpl exampleResourceImpl = new ExampleResourceImpl(
            exampleEventPublisher, exampleSupportResource);
        channel.serviceSupport.init(exampleResourceImpl);
        
        // client
        final ExampleStub exampleResource = channel.serviceClientSupport.getOrCreate(ExampleStub.class).createProxy();
        
        for (int i = 0; i < 50; i++) {
            LOG.info("---------------------- Starting new test (" + i + ") -----------------------");
            initExampleSupport();
            
            final ServiceOperationHeader<ExampleContext> header = TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build()).build());
            final CompletionStage<ExampleResponse> p = exampleResource.op(header, ExampleRequest.newBuilder().setId(i).build());
            
            try {
                final ExampleResponse result = join(p);
                Assertions.assertThat(result.getId()).isEqualTo(i);
            } catch (final ServiceException e) {
                if (e.getStatusCode() == ResponseStatusCode.SERVER_TIMEOUT) {
                    Assertions.fail("TIMEOUT FAILURE! - Call to ExampleResource failed within iteration number: " + i, e);
                } else {
                    Assertions.fail("Call to ExampleResource failed", e);
                }
            } catch (final Throwable t) {
                Assertions.fail("Call to ExampleResource failed", t);
            }
            
            LOG.info("---------------------- Destroying support example -----------------------");
            destroyExampleSupport();
            
            final CompletionStage<ExampleResponse> p2 = exampleResource.op(header, ExampleRequest.newBuilder().setId(i).build());
            
            try {
                join(p2);
                Assertions.fail("Should fail as there is no service");
            } catch (final ServiceException e) {
                if (e.getStatusCode() != ResponseStatusCode.SERVER_UNAVAILABLE) {
                    Assertions.fail("Call to ExampleResource failed", e);
                }
            } catch (final Throwable t) {
                Assertions.fail("Call to ExampleResource failed", t);
            }
        }
        
        channel.serviceSupport.destroy(exampleResourceImpl);
    }
    
    private void initExampleSupport() {
        exampleSupportSkeleton = new ExampleSupportSkeletonImpl();
        channel.serviceSupport.init(exampleSupportSkeleton);
    }
    
    private void destroyExampleSupport() {
        channel.serviceSupport.destroy(exampleSupportSkeleton);
    }
    
}
