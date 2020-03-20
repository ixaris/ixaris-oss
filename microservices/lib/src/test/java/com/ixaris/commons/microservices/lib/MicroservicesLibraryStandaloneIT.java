package com.ixaris.commons.microservices.lib;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.example.client.ExampleServiceClientNoEventsImpl;
import com.ixaris.commons.microservices.lib.example.client.ExampleStub;
import com.ixaris.commons.microservices.lib.example.service.ExampleSkeleton;
import com.ixaris.commons.microservices.lib.example.service.ExampleSkeleton.NestedSkeleton;
import com.ixaris.commons.microservices.lib.example.service.ExampleSkeletonImpl;
import com.ixaris.commons.microservices.lib.local.LocalEvents;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.local.LocalServiceClientSupport;
import com.ixaris.commons.microservices.lib.local.LocalServiceSupport;
import com.ixaris.commons.microservices.lib.localstack.TestServiceSecurityChecker;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.microservices.lib.service.support.DefaultServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.misc.lib.startup.StartupTask;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

public class MicroservicesLibraryStandaloneIT {
    
    @BeforeClass
    public static void beforeClass() {
        StartupTask.loadAndRunTasks();
    }
    
    private MultiTenancy multiTenancy;
    
    @Before
    public void setup() {
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        multiTenancy.addTenant(MultiTenancy.SYSTEM_TENANT);
        multiTenancy.addTenant(TestTenants.DEFAULT);
    }
    
    @After
    public void tearDown() {
        multiTenancy.stop();
    }
    
    @Test
    @Ignore("Benchmark test is normally ignored and re-enabled when needed")
    public void testBenchmark() {
        final LocalService serviceDiscovery = new LocalService(Collections.emptySet(), Collections.emptySet());
        final TestServiceSecurityChecker securityChecker = new TestServiceSecurityChecker();
        
        final LocalOperations localOperations = new LocalOperations(AsyncExecutor.DEFAULT);
        final LocalEvents localEvents = new LocalEvents(AsyncExecutor.DEFAULT);
        
        final LocalServiceClientSupport localServiceClientSupport = new LocalServiceClientSupport(
            AsyncExecutor.DEFAULT,
            multiTenancy,
            Defaults.DEFAULT_TIMEOUT,
            localOperations,
            localEvents,
            serviceDiscovery,
            ServiceAsyncInterceptor.PASSTHROUGH,
            ServiceHandlerStrategy.PASSTHROUGH,
            Collections.emptySet());
        final LocalServiceSupport localServiceSupport = new LocalServiceSupport(
            multiTenancy,
            localOperations,
            localEvents,
            serviceDiscovery,
            securityChecker,
            ServiceAsyncInterceptor.PASSTHROUGH,
            ServiceHandlerStrategy.PASSTHROUGH,
            Collections.emptySet(),
            Collections.emptySet(),
            new DefaultServiceKeys());
        
        // service
        final ServiceSkeletonProxy<ExampleSkeleton> proxy = localServiceSupport.getOrCreate(ExampleSkeleton.class);
        final ExampleSkeletonImpl exampleServiceImpl = new ExampleSkeletonImpl(proxy.createPublisherProxy(
            NestedSkeleton.Watch.class));
        localServiceSupport.init(exampleServiceImpl);
        
        // client
        final ExampleStub exampleResource = localServiceClientSupport.getOrCreate(ExampleStub.class).createProxy();
        final ExampleServiceClientNoEventsImpl client = new ExampleServiceClientNoEventsImpl(
            exampleResource,
            proxy,
            exampleServiceImpl);
        
        client.benchmarkDirect(1000000);
        client.benchmarkInvoke(1000000);
        client.benchmarkMessages(1000000);
        
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {}
        
        localServiceSupport.destroy(exampleServiceImpl);
    }
    
}
