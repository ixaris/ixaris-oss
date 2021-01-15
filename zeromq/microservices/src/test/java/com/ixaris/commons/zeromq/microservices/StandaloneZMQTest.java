package com.ixaris.commons.zeromq.microservices;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.CompletionStageUtil;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.example.client.ExampleServiceClientImpl;
import com.ixaris.commons.zeromq.microservices.example.client.ExampleSpiServiceClientImpl;
import com.ixaris.commons.zeromq.microservices.example.client.ExampleStub;
import com.ixaris.commons.zeromq.microservices.example.service.Alt2ExampleZmqSpiResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.Alt3ExampleZmqSpiResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.AltExampleZmqSpiResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleSkeleton;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleSupportSkeletonImpl;
import com.ixaris.commons.zeromq.microservices.examplespi.client.ExampleSpiStub;
import com.ixaris.commons.zeromq.microservices.examplesupport.client.ExampleSupportStub;
import com.ixaris.commons.zeromq.microservices.stack.TestServiceSecurityChecker;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceChannel;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceClientSupport;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceSupport;

public class StandaloneZMQTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(StandaloneZMQTest.class);
    private static final int START_PORT = 39000;
    
    private static MultiTenancy multiTenancy;
    private static TestZMQServiceChannel channel;
    private static ExampleServiceClientImpl client;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<ServiceSkeleton> INSTANCES = new HashSet<>();
    
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
        
        // service 1
        final ExampleSupportSkeletonImpl exampleSupportSkeleton = new ExampleSupportSkeletonImpl();
        channel.serviceSupport.init(exampleSupportSkeleton);
        INSTANCES.add(exampleSupportSkeleton);
        
        // service 1
        final ServiceSkeletonProxy<ExampleSkeleton> proxy = channel.serviceSupport.getOrCreate(ExampleSkeleton.class);
        final ExampleSkeleton.Watch exampleEventPublisher = proxy.createPublisherProxy(ExampleSkeleton.Watch.class);
        final ExampleSupportStub exampleSupportResource = channel.serviceClientSupport
            .getOrCreate(ExampleSupportStub.class)
            .createProxy();
        final ExampleResourceImpl exampleResourceImpl = new ExampleResourceImpl(
            exampleEventPublisher, exampleSupportResource);
        channel.serviceSupport.init(exampleResourceImpl);
        INSTANCES.add(exampleResourceImpl);
        
        // service ALT
        final AltExampleZmqSpiResourceImpl exampleSpiImpl = new AltExampleZmqSpiResourceImpl();
        channel.serviceSupport.init(exampleSpiImpl);
        INSTANCES.add(exampleSpiImpl);
        
        // service ALT2
        final Alt2ExampleZmqSpiResourceImpl exampleSpiImpl2 = new Alt2ExampleZmqSpiResourceImpl();
        channel.serviceSupport.init(exampleSpiImpl2);
        INSTANCES.add(exampleSpiImpl2);
        
        // service ALT3
        final Alt3ExampleZmqSpiResourceImpl exampleSpiImpl3 = new Alt3ExampleZmqSpiResourceImpl();
        channel.serviceSupport.init(exampleSpiImpl3);
        INSTANCES.add(exampleSpiImpl3);
        
        // client
        final ExampleStub exampleResource = channel.serviceClientSupport.getOrCreate(ExampleStub.class).createProxy();
        client = new ExampleServiceClientImpl(exampleResource);
    }
    
    @AfterClass
    public static void teardownClass() {
        INSTANCES.forEach(i -> channel.serviceSupport.destroy(i));
        INSTANCES.clear();
        
        multiTenancy.stop();
        
        ZMQGlobal.shutdown();
    }
    
    @Before
    public void setup() {
        ExampleServiceClientImpl.log = true;
        ExampleResourceImpl.log = true;
    }
    
    @Test
    public void test_single_call_with_future() throws Exception {
        
        LOG.info("Single call with future");
        
        final int id = RANDOM.nextInt();
        
        final ExampleResponse response = block(client.doSomethingWithPromise(id, 0, false));
        LOG.info("Received response: {}", response);
        
        assertEquals(id, response.getId());
    }
    
    @Test
    public void test_MultipleSpi_call() throws Exception {
        
        final int id = RANDOM.nextInt();
        
        // client
        final ExampleSpiStub exampleZmqSpiResource = channel.serviceClientSupport.getOrCreate(ExampleSpiStub.class).createProxy();
        ExampleSpiServiceClientImpl spiServiceClient = new ExampleSpiServiceClientImpl(exampleZmqSpiResource);
        
        Thread.sleep(500L);
        
        // work
        final CompletionStage<List<ExampleResponse>> p = spiServiceClient.doSomething(id, 5000, 10, false);
        assertNotNull("SPI should have at least one available implementation", p);
        
        p.toCompletableFuture().join();
        
        // work
        final CompletionStage<List<ExampleResponse>> p2 = spiServiceClient.doSomething(id, 5000, 10, false);
        assertNotNull("SPI should have at least one available implementation", p2);
        
        p2.toCompletableFuture().join();
    }
    
    @Test
    public void test_timed_out_call_with_future() throws Exception {
        
        final int id = RANDOM.nextInt();
        
        // Send a request with 100ms deadline/timeout
        try {
            block(client.doSomethingWithPromise(id, 5100, false));
            
            fail("Operation request should have failed due to timeout");
        } catch (final ServiceException e) {
            assertEquals("response Code after timeout should be SERVER_TIMEOUT", ResponseStatusCode.SERVER_TIMEOUT, e.getStatusCode());
        }
    }
    
    @Test
    public void test_failed_call_service_error_with_future() throws Exception {
        
        final int id = RANDOM.nextInt();
        
        final CompletionStage<ExampleResponse> p = client.doSomethingWithPromise(id, 0, true);
        
        try {
            CompletionStageUtil.block(p);
            
            fail("Operation request should have failed due to exception");
        } catch (final ServiceException e) {
            assertEquals(ResponseStatusCode.SERVER_ERROR, e.getStatusCode());
            assertEquals("RuntimeException Service-side exception thrown", e.getMessage());
        }
    }
    
    @Test
    public void test_failed_call_service_error_with_callback() throws Exception {
        
        final int id = RANDOM.nextInt();
        
        try {
            block(client.doSomethingWithPromise(id, 0, true));
            
            fail("Operation request should have failed due to timeout");
        } catch (final ServiceException e) {
            assertEquals(ResponseStatusCode.SERVER_ERROR, e.getStatusCode());
            assertEquals("RuntimeException Service-side exception thrown", e.getMessage());
        }
        
        final AtomicBoolean finallyProcessed = new AtomicBoolean(false);
        
        try {
            block(client.doSomethingWithPromise(id, 0, true));
            fail("Operation should have failed due to server fail");
        } catch (final ServiceException e) {
            assertEquals(ResponseStatusCode.SERVER_ERROR, e.getStatusCode());
            assertEquals("RuntimeException Service-side exception thrown", e.getMessage());
            finallyProcessed.set(true);
        }
        
        // sleep at most 500ms, early exit if finally processed
        int sleepCount = 0;
        while (!finallyProcessed.get() && sleepCount < 5) {
            sleepCount += 1;
            Thread.sleep(100);
        }
        
        assertTrue(finallyProcessed.get());
    }
    
    @Test
    public void test_timed_out_call_with_callback() throws Exception {
        
        final int id = RANDOM.nextInt();
        
        final AtomicBoolean finallyProcessed = new AtomicBoolean(false);
        
        try {
            block(client.doSomethingWithPromise(id, 5100, false));
            fail("Operation should have failed due to timeout");
        } catch (final ServiceException e) {
            assertEquals(ResponseStatusCode.SERVER_TIMEOUT, e.getStatusCode());
            finallyProcessed.set(true);
        }
        // sleep at most 500ms, early exit if finally processed
        int sleepCount = 0;
        while (!finallyProcessed.get() && sleepCount < 5) {
            sleepCount += 1;
            Thread.sleep(100);
        }
        
        assertTrue(finallyProcessed.get());
        
        Thread.sleep(1000);
    }
    
    @Test
    public void test_single_call_with_callbacks() throws Exception {
        
        final int id = RANDOM.nextInt();
        final AtomicInteger responseId = new AtomicInteger(-1);
        
        final AtomicBoolean finallyProcessed = new AtomicBoolean(false);
        
        try {
            final ExampleResponse t = block(client.doSomethingWithPromise(id, 0, false));
            responseId.set(t.getId());
            finallyProcessed.set(true);
        } catch (final Throwable t) {
            fail("Operation request failed due to " + t);
        }
        
        // sleep at most 500ms, early exit if response is obtained
        int sleepCount = 0;
        while (responseId.get() == -1 && sleepCount < 5) {
            sleepCount += 1;
            Thread.sleep(500);
        }
        
        assertEquals("Unexpected response id", id, responseId.get());
        assertTrue("Request should be processed", finallyProcessed.get());
    }
    
}
