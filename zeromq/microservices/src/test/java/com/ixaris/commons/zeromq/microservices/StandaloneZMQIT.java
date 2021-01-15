package com.ixaris.commons.zeromq.microservices;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.example.client.Example3ServiceClientImpl;
import com.ixaris.commons.zeromq.microservices.example.client.ExampleServiceClientImpl;
import com.ixaris.commons.zeromq.microservices.example.client.ExampleStub;
import com.ixaris.commons.zeromq.microservices.example.service.Alt2ExampleZmqSpiResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.Alt3ExampleZmqSpiResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.AltExampleZmqSpiResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.Example3ResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleSkeleton;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleSupportSkeletonImpl;
import com.ixaris.commons.zeromq.microservices.example3.client.Example3Stub;
import com.ixaris.commons.zeromq.microservices.examplesupport.client.ExampleSupportStub;
import com.ixaris.commons.zeromq.microservices.stack.TestServiceSecurityChecker;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceChannel;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceClientSupport;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceSupport;

public class StandaloneZMQIT {
    
    private static final Logger LOG = LoggerFactory.getLogger(StandaloneZMQIT.class);
    
    private static final int START_PORT = 39000;
    private static final long CONCURRENT_TEST_TIMEOUT = 60000L;
    private static MultiTenancy multiTenancy;
    private static TestZMQServiceChannel channel;
    private static ExampleServiceClientImpl client;
    private static final Set<ServiceSkeleton> INSTANCES = new HashSet<>();
    
    @BeforeClass
    public static void setupClass() {
        ZMQGlobal.start();
        
        final TestServiceSecurityChecker testServiceSecurityChecker = new TestServiceSecurityChecker();
        final LocalService localService = new LocalService(Collections.emptySet(), Collections.emptySet());
        
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        
        channel = new TestZMQServiceChannel(new TestZMQServiceSupport(multiTenancy, START_PORT, localService, testServiceSecurityChecker, false),
            new TestZMQServiceClientSupport(multiTenancy, localService, false));
        
        multiTenancy.addTenant(TestTenants.DEFAULT);
        
        final ExampleSupportSkeletonImpl exampleSupportSkeleton = new ExampleSupportSkeletonImpl();
        channel.serviceSupport.init(exampleSupportSkeleton);
        INSTANCES.add(exampleSupportSkeleton);
        
        // service 1
        final ServiceSkeletonProxy<ExampleSkeleton> proxy = channel.serviceSupport.getOrCreate(ExampleSkeleton.class);
        final ExampleSkeleton.Watch exampleEventPublisher = proxy.createPublisherProxy(ExampleSkeleton.Watch.class);
        final ExampleSupportStub exampleSupportResource = channel.serviceClientSupport
            .getOrCreate(ExampleSupportStub.class)
            .createProxy();
        final ExampleResourceImpl exampleResourceImpl = new ExampleResourceImpl(exampleEventPublisher, exampleSupportResource);
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
    public void test_shouldProcessRequests_WithoutExceptions() throws Throwable {
        join(client.doSomething());
    }
    
    @Test
    public void test_concurrent_calls() throws InterruptedException {
        Thread.sleep(500L);
        
        do_test_concurrent_calls(5, 1000);
        do_test_concurrent_calls(50, 100);
        do_test_concurrent_calls(250, 20);
        
        Thread.sleep(2000L);
    }
    
    private void do_test_concurrent_calls(final int THREAD_COUNT, final int CALL_COUNT) throws InterruptedException {
        ExampleServiceClientImpl.log = false;
        ExampleResourceImpl.log = false;
        
        final AtomicInteger completedThreadsCount = new AtomicInteger(0);
        final AtomicInteger errorThreadsCount = new AtomicInteger(0);
        final AtomicInteger completedCallCount = new AtomicInteger(0);
        final Set<Runnable> running = new HashSet<>();
        
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < THREAD_COUNT; i++) {
            
            final int fi = i;
            
            new Thread(new Runnable() {
                
                private int ii = fi;
                
                @Override
                public void run() {
                    synchronized (running) {
                        running.add(this);
                    }
                    
                    try {
                        for (int jj = 0; jj < CALL_COUNT; jj++) {
                            
                            final ExampleResponse response = block(client.doSomethingWithPromise(jj, 0, false));
                            
                            completedCallCount.incrementAndGet();
                            
                            assertEquals("Received id in response does not match the ID in the original request", jj, response.getId());
                        }
                        
                    } catch (final Throwable e) {
                        errorThreadsCount.incrementAndGet();
                        LOG.error("Exception while sending concurrent requests", e);
                        
                    } finally {
                        completedThreadsCount.incrementAndGet();
                        synchronized (running) {
                            running.remove(this);
                        }
                    }
                }
            }).start();
        }
        
        while (completedThreadsCount.get() < THREAD_COUNT) {
            Thread.sleep(250L);
            
            if (System.currentTimeMillis() - startTime > CONCURRENT_TEST_TIMEOUT) {
                LOG.info("Concurrent requests timed out");
                fail("Concurrent requests timed out");
            }
            LOG.info("Completed so far threads | calls | errors : {} | {} | {}",
                completedThreadsCount.get(),
                completedCallCount.get(),
                errorThreadsCount.get());
        }
        
        LOG.info("Completed threads | calls | errors: {} | {} | {} in {}ms",
            completedThreadsCount.get(),
            completedCallCount.get(),
            errorThreadsCount.get(),
            System.currentTimeMillis() - startTime);
        
        if (errorThreadsCount.get() > 0) {
            fail("A number of calls [" + errorThreadsCount.get() + "] failed");
        }
    }
    
    @Test
    @Ignore
    public void testBenchmark() throws InterruptedException {
        
        // service 2
        final Example3ResourceImpl example3ResourceImpl = new Example3ResourceImpl();
        channel.serviceSupport.init(example3ResourceImpl);
        
        // client
        final Example3Stub example3Resource = channel.serviceClientSupport.getOrCreate(Example3Stub.class).createProxy();
        final Example3ServiceClientImpl client3 = new Example3ServiceClientImpl(example3Resource);
        
        Thread.sleep(200L);
        
        client3.benchmark(10000);
        
        Thread.sleep(1000L);
    }
    
}
