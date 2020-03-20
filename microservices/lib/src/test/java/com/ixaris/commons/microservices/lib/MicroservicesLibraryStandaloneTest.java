package com.ixaris.commons.microservices.lib;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.protobuf.ByteString;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.microservices.lib.client.dynamic.DynamicServiceFactory;
import com.ixaris.commons.microservices.lib.client.proxy.UntypedOperationInvoker;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientLoggingFilterFactory;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.exception.ServerErrorException;
import com.ixaris.commons.microservices.lib.common.exception.ServerNotImplementedException;
import com.ixaris.commons.microservices.lib.common.exception.ServerTimeoutException;
import com.ixaris.commons.microservices.lib.common.exception.ServerUnavailableException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.example.Example.ExampleContext;
import com.ixaris.commons.microservices.lib.example.Example.ExampleError.ExampleErrorCode;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest.FailureType;
import com.ixaris.commons.microservices.lib.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.lib.example.client.ExampleServiceClientImpl;
import com.ixaris.commons.microservices.lib.example.client.ExampleStub;
import com.ixaris.commons.microservices.lib.example.resource.ExampleResource.ExampleErrorException;
import com.ixaris.commons.microservices.lib.example.service.ExampleSkeleton;
import com.ixaris.commons.microservices.lib.example.service.ExampleSkeleton.NestedSkeleton;
import com.ixaris.commons.microservices.lib.example.service.ExampleSkeletonImpl;
import com.ixaris.commons.microservices.lib.example2.client.Example2ServiceClientImpl;
import com.ixaris.commons.microservices.lib.example2.client.Example2Stub;
import com.ixaris.commons.microservices.lib.example2.service.Example2SkeletonImpl;
import com.ixaris.commons.microservices.lib.examplespi.client.ExampleSpiServiceClientImpl;
import com.ixaris.commons.microservices.lib.examplespi.client.ExampleSpiStub;
import com.ixaris.commons.microservices.lib.examplespi.service.Alt2ExampleSpiSkeletonImpl;
import com.ixaris.commons.microservices.lib.examplespi.service.AltExampleSpiSkeletonImpl;
import com.ixaris.commons.microservices.lib.local.LocalEvents;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.local.LocalServiceClientSupport;
import com.ixaris.commons.microservices.lib.local.LocalServiceSupport;
import com.ixaris.commons.microservices.lib.localstack.TestServiceSecurityChecker;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.microservices.lib.service.support.DefaultServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceLoggingFilterFactory;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.misc.lib.function.BiConsumerThrows;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.misc.lib.startup.StartupTask;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

public class MicroservicesLibraryStandaloneTest {
    
    @BeforeClass
    public static void beforeClass() {
        StartupTask.loadAndRunTasks();
    }
    
    private static final String INVALID_TENANT = "invalid";
    
    private MultiTenancy multiTenancy;
    private LocalServiceSupport localServiceSupport;
    private LocalServiceClientSupport localServiceClientSupport;
    private final Random random = new Random();
    
    @Before
    public void setup() {
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        multiTenancy.addTenant(MultiTenancy.SYSTEM_TENANT);
        multiTenancy.addTenant(TestTenants.DEFAULT);
        
        final LocalService localService = new LocalService(Collections.emptySet(), Collections.emptySet());
        final TestServiceSecurityChecker serviceSecurityChecker = new TestServiceSecurityChecker();
        final LocalOperations localOperations = new LocalOperations(AsyncExecutor.DEFAULT);
        final LocalEvents localEvents = new LocalEvents(AsyncExecutor.DEFAULT);
        localServiceClientSupport = new LocalServiceClientSupport(
            AsyncExecutor.DEFAULT,
            multiTenancy,
            Defaults.DEFAULT_TIMEOUT,
            localOperations,
            localEvents,
            localService,
            ServiceAsyncInterceptor.PASSTHROUGH,
            ServiceHandlerStrategy.PASSTHROUGH,
            Collections.singleton(new ServiceClientLoggingFilterFactory()));
        localServiceSupport = new LocalServiceSupport(
            multiTenancy,
            localOperations,
            localEvents,
            localService,
            serviceSecurityChecker,
            ServiceAsyncInterceptor.PASSTHROUGH,
            ServiceHandlerStrategy.PASSTHROUGH,
            Collections.emptySet(),
            Collections.singleton(new ServiceLoggingFilterFactory()),
            new DefaultServiceKeys());
    }
    
    @After
    public void tearDown() {
        multiTenancy.stop();
    }
    
    @Test
    public void initServices_sendSomeCalls_shouldReceiveSuccessfulResponses() throws Exception {
        
        // client
        final ExampleStub example = localServiceClientSupport.getOrCreate(ExampleStub.class).createProxy();
        final Example2Stub example2 = localServiceClientSupport.getOrCreate(Example2Stub.class).createProxy();
        
        final ExampleServiceClientImpl client = new ExampleServiceClientImpl(example);
        final Example2ServiceClientImpl client2 = new Example2ServiceClientImpl(example2);
        
        // work before add
        try {
            block(client.sendChainedRequestsAndSimulateWork());
        } catch (final ServerUnavailableException expected) {}
        
        // service 1
        final ServiceSkeletonProxy<ExampleSkeleton> proxy = localServiceSupport.getOrCreate(ExampleSkeleton.class);
        final ExampleSkeletonImpl exampleServiceImpl = new ExampleSkeletonImpl(proxy.createPublisherProxy(
            NestedSkeleton.Watch.class));
        localServiceSupport.init(exampleServiceImpl);
        
        // service 2
        final Example2SkeletonImpl example2ServiceImpl = new Example2SkeletonImpl();
        localServiceSupport.init(example2ServiceImpl);
        
        // service 3
        final AltExampleSpiSkeletonImpl exampleServiceImpl_ = new AltExampleSpiSkeletonImpl();
        localServiceSupport.init(exampleServiceImpl_);
        
        try {
            // work
            block(client.sendChainedRequestsAndSimulateWork());
            block(client2.sendChainedRequestsAndSimulateWork());
        } finally {
            localServiceSupport.destroy(exampleServiceImpl);
            localServiceSupport.destroy(example2ServiceImpl);
            localServiceSupport.destroy(exampleServiceImpl_);
        }
    }
    
    @Test
    public void sendRequestOnIdResource_shouldBeSuccessful() throws Throwable {
        withServiceClients((client, client2) -> {
            block(client.sendRequestOnIdResource(random.nextInt(), 1000));
            block(client2.sendRequestOnIdResource(random.nextInt(), 1000));
        });
    }
    
    @Test
    public void testEventBuildPublish() throws Throwable {
        withServiceClients((client, client2) -> {
            final int id = random.nextInt();
            final ExampleResponse r = block(client.sendRequest(id, 5000), 5, TimeUnit.SECONDS);
            assertEquals(id, r.getId(), "Id must be equal");
        });
    }
    
    @Test(expected = ServiceException.class)
    public void securedOperation_invalidSecurityDetails_shouldReturnClientUnauthorised() throws Throwable {
        withServiceClients((client, client2) -> {
            try {
                block(client.sendSecuredRequest(1, "NO"));
            } catch (final ServiceException e) {
                assertThat(e.getStatusCode()).isEqualTo(ResponseStatusCode.CLIENT_UNAUTHORISED);
                throw e;
            }
        });
    }
    
    @Test
    public void testRequest_invalidTenant_serverUnavailable() throws Throwable {
        withServiceClients((client, client2) -> {
            try {
                block(client.sendRequest(random.nextInt(), 1000, INVALID_TENANT));
                fail("Call on an invalid tenant should fail!");
            } catch (final ServiceException e) {
                assertThat(e).isInstanceOf(ServerUnavailableException.class);
                assertThat(e.getMessage()).containsIgnoringCase(format("tenant [%s]", INVALID_TENANT));
                assertThat(e.getStatusCode()).isEqualTo(ResponseStatusCode.SERVER_UNAVAILABLE);
            }
        });
    }
    
    @Test
    public void testRequest_runtimeException_serverError() throws Throwable {
        withServiceClients((client, client2) -> {
            try {
                block(client.sendRequestWithFailureType(random.nextInt(), 1000, FailureType.RUNTIME));
                fail("Call with runtime exception should fail!");
            } catch (final ServiceException e) {
                assertThat(e).isInstanceOf(ServerErrorException.class);
                assertThat(e.getStatusCode()).isEqualTo(ResponseStatusCode.SERVER_ERROR);
            }
        });
    }
    
    @Test
    public void testRequest_notImplemented() throws Throwable {
        withServiceClients((client, client2) -> {
            try {
                block(client.sendRequestWithFailureType(random.nextInt(), 1000, FailureType.NOT_IMPLEMENTED));
                fail("Call on a not implemented operation should fail!");
            } catch (final ServiceException e) {
                assertThat(e).isInstanceOf(ServerNotImplementedException.class);
                assertThat(e.getStatusCode()).isEqualTo(ResponseStatusCode.SERVER_NOT_IMPLEMENTED);
            }
        });
    }
    
    @Test
    public void testRequest_unsupportedOperation_notImplemented() throws Throwable {
        withServiceClients((client, client2) -> {
            try {
                block(client.sendRequestWithFailureType(random.nextInt(), 1000, FailureType.UNSUPPORTED_OP));
                fail("Call on an usupported operation should fail!");
            } catch (final ServiceException e) {
                assertThat(e).isInstanceOf(ServerNotImplementedException.class);
                assertThat(e.getStatusCode()).isEqualTo(ResponseStatusCode.SERVER_NOT_IMPLEMENTED);
            }
        });
    }
    
    @Test
    public void testRequest_conflict() throws Throwable {
        withServiceClients((client, client2) -> {
            try {
                block(client.sendRequestWithFailureType(random.nextInt(), 1000, FailureType.CONFLICT));
                fail("Call with a conflict failure should fail!");
            } catch (final ExampleErrorException e) {
                assertThat(e.getConflict().getErrorCode()).isEqualTo(ExampleErrorCode.EXAMPLE_ERROR);
            }
        });
    }
    
    @Test
    public void testRequest_timeout() throws Throwable {
        withServiceClients((client, client2) -> {
            try {
                block(client.sendRequestWithFailureType(random.nextInt(), 1000, FailureType.TIMEOUT));
                fail("Call with a timeout should fail!");
            } catch (final ServiceException e) {
                assertThat(e).isInstanceOf(ServerTimeoutException.class);
                assertThat(e.getStatusCode()).isEqualTo(ResponseStatusCode.SERVER_TIMEOUT);
            }
        });
    }
    
    @Test
    public void securedOperation_correctSecurityDetails_shouldBeSuccessful() throws Throwable {
        withServiceClients((client, client2) -> {
            try {
                block(client.sendSecuredRequest(1, "TEST"));
            } catch (final ServiceException e) {
                Assertions.fail("Call to a secured service with expected security details should be successful", e);
            }
        });
    }
    
    private void withServiceClients(BiConsumerThrows<ExampleServiceClientImpl, Example2ServiceClientImpl, Throwable> consumer) throws Throwable {
        // client
        final ExampleStub example = localServiceClientSupport.getOrCreate(ExampleStub.class).createProxy();
        final Example2Stub example2 = localServiceClientSupport.getOrCreate(Example2Stub.class).createProxy();
        
        final ExampleServiceClientImpl client = new ExampleServiceClientImpl(example);
        final Example2ServiceClientImpl client2 = new Example2ServiceClientImpl(example2);
        
        // service 1
        final ServiceSkeletonProxy<ExampleSkeleton> proxy = localServiceSupport.getOrCreate(ExampleSkeleton.class);
        final ExampleSkeletonImpl exampleServiceImpl = new ExampleSkeletonImpl(proxy.createPublisherProxy(
            NestedSkeleton.Watch.class));
        localServiceSupport.init(exampleServiceImpl);
        
        // service 2
        final Example2SkeletonImpl example2ServiceImpl = new Example2SkeletonImpl();
        localServiceSupport.init(example2ServiceImpl);
        
        try {
            consumer.accept(client, client2);
        } finally {
            localServiceSupport.destroy(exampleServiceImpl);
        }
    }
    
    @Test
    public void testRequests_DynamicServiceFactory() throws Throwable {
        
        // service 1
        final ServiceSkeletonProxy<ExampleSkeleton> proxy = localServiceSupport.getOrCreate(ExampleSkeleton.class);
        final ExampleSkeletonImpl exampleServiceImpl = new ExampleSkeletonImpl(proxy.createPublisherProxy(
            NestedSkeleton.Watch.class));
        localServiceSupport.init(exampleServiceImpl);
        
        // service 2
        final Example2SkeletonImpl example2ServiceImpl = new Example2SkeletonImpl();
        localServiceSupport.init(example2ServiceImpl);
        
        try {
            final DynamicServiceFactory dynamicServiceFactory = new DynamicServiceFactory(
                localServiceClientSupport,
                true);
            final UntypedOperationInvoker exampleProxy = block(dynamicServiceFactory.getProxy("example"));
            final UntypedOperationInvoker example2Proxy = block(dynamicServiceFactory.getProxy("example2"));
            
            final RequestEnvelope.Builder builder1 = RequestEnvelope.newBuilder()
                .setCorrelationId(UniqueIdGenerator.generate())
                .setMethod("example_operation")
                .setContext(ExampleContext.newBuilder().build().toByteString())
                .setJsonPayload(true)
                .setPayload(ByteString.copyFromUtf8("{ \"id\": 123 }"))
                .setTenantId(TestTenants.DEFAULT)
                .setIntentId(UniqueIdGenerator.generate())
                .setTimeout(1000);
            
            final Async<ResponseEnvelope> r1 = exampleProxy.invoke(builder1.build());
            assertThat(block(r1).getPayload().toStringUtf8().contains("\"id\": 123")).isTrue();
            
            final RequestEnvelope.Builder builder2 = RequestEnvelope.newBuilder()
                .setCorrelationId(UniqueIdGenerator.generate())
                .setMethod("example_operation2")
                .setContext(ExampleContext.newBuilder().build().toByteString())
                .setJsonPayload(true)
                .setPayload(ByteString.copyFromUtf8("{ \"id\": 123 }"))
                .setTenantId(TestTenants.DEFAULT)
                .setIntentId(UniqueIdGenerator.generate())
                .setTimeout(1000);
            
            final Async<ResponseEnvelope> r2 = example2Proxy.invoke(builder2.build());
            assertThat(block(r2, 1000, TimeUnit.MILLISECONDS).getPayload().toStringUtf8().contains("\"id\": 123"))
                .isTrue();
            
            final RequestEnvelope.Builder builder3 = RequestEnvelope.newBuilder()
                .setCorrelationId(UniqueIdGenerator.generate())
                .setMethod("non_existent")
                .setContext(ExampleContext.newBuilder().build().toByteString())
                .setJsonPayload(true)
                .setPayload(ByteString.copyFromUtf8("{ \"id\": 123 }"))
                .setTenantId(TestTenants.DEFAULT)
                .setIntentId(UniqueIdGenerator.generate())
                .setTimeout(1000);
            
            final Async<ResponseEnvelope> r3 = exampleProxy.invoke(builder3.build());
            assertThat(block(r3).getStatusCode()).isEqualTo(ResponseStatusCode.CLIENT_METHOD_NOT_ALLOWED);
        } finally {
            localServiceSupport.destroy(exampleServiceImpl);
        }
    }
    
    @Test
    public void testSpis() throws Exception {
        
        // client
        final ExampleSpiStub exampleSpiResource = localServiceClientSupport
            .getOrCreate(ExampleSpiStub.class)
            .createProxy();
        final ExampleSpiServiceClientImpl spiServiceClient = new ExampleSpiServiceClientImpl(exampleSpiResource);
        
        // service 3
        final AltExampleSpiSkeletonImpl exampleServiceImpl_ = new AltExampleSpiSkeletonImpl();
        localServiceSupport.init(exampleServiceImpl_);
        
        try {
            // work
            block(spiServiceClient.doSomethingOnAll(1, 500, 10, false));
        } finally {
            localServiceSupport.destroy(exampleServiceImpl_);
        }
    }
    
    @Test
    public void testSpis_MultipleSpis() throws Exception {
        
        // client
        final ExampleSpiStub exampleSpiResource = localServiceClientSupport
            .getOrCreate(ExampleSpiStub.class)
            .createProxy();
        final ExampleSpiServiceClientImpl spiServiceClient = new ExampleSpiServiceClientImpl(exampleSpiResource);
        
        // service 3
        final AltExampleSpiSkeletonImpl exampleServiceImpl_ = new AltExampleSpiSkeletonImpl();
        localServiceSupport.init(exampleServiceImpl_);
        
        final Alt2ExampleSpiSkeletonImpl exampleServiceImpl2_ = new Alt2ExampleSpiSkeletonImpl();
        localServiceSupport.init(exampleServiceImpl2_);
        
        Thread.sleep(500L);
        
        assertThat(exampleSpiResource._keys()).containsExactlyInAnyOrder("ALT", "ALT2", "ALT3");
        // work
        block(spiServiceClient.doSomethingOnAll(1, 500, 10, false));
        
        localServiceSupport.destroy(exampleServiceImpl2_);
        localServiceSupport.destroy(exampleServiceImpl_);
    }
    
}
