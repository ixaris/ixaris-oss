package com.ixaris.commons.microservices.spring;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.local.LocalEvents;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.local.LocalServiceClientSupport;
import com.ixaris.commons.microservices.lib.local.LocalServiceSupport;
import com.ixaris.commons.microservices.lib.service.support.DefaultServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;
import com.ixaris.commons.microservices.spring.example.client.ExampleServiceClientSpringImpl;
import com.ixaris.commons.microservices.spring.example.client.ExampleSpiServiceClientSpringImpl;
import com.ixaris.commons.microservices.spring.localstack.TestServiceSecurityChecker;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class MicroservicesSetupWithSpringIT {
    
    @Configuration
    @ComponentScan({ "com.ixaris.commons.microservices.spring", "com.ixaris.commons.multitenancy.spring" })
    static class ContextConfiguration {
        
        @Bean
        public LocalService localService() {
            return new LocalService(Collections.emptySet(), Collections.emptySet());
        }
        
        @Bean
        public ServiceSecurityChecker serviceSecurityChecker() {
            return new TestServiceSecurityChecker();
        }
        
        @Bean
        public LocalOperations localOperations() {
            return new LocalOperations(AsyncExecutor.DEFAULT);
        }
        
        @Bean
        public LocalEvents localEvents() {
            return new LocalEvents(AsyncExecutor.DEFAULT);
        }
        
        @Bean
        public DefaultServiceKeys defaultServiceKeys() {
            return new DefaultServiceKeys();
        }
        
        @Bean
        public LocalServiceSupport localMicroservicesSupport(final MultiTenancy multiTenancy,
                                                             final LocalOperations localOperations,
                                                             final LocalEvents localEvents,
                                                             final LocalService localService,
                                                             final ServiceSecurityChecker serviceSecurityChecker,
                                                             final ServiceKeys serviceKeys) {
            return new LocalServiceSupport(multiTenancy,
                localOperations,
                localEvents,
                localService,
                serviceSecurityChecker,
                ServiceAsyncInterceptor.PASSTHROUGH,
                ServiceHandlerStrategy.PASSTHROUGH,
                Collections.emptySet(),
                Collections.emptySet(),
                serviceKeys);
        }
        
        @Bean
        public LocalServiceClientSupport localMicroservicesClientSupport(final MultiTenancy multiTenancy,
                                                                         final LocalOperations localOperations,
                                                                         final LocalEvents localEvents,
                                                                         final LocalService localService) {
            return new LocalServiceClientSupport(AsyncExecutor.DEFAULT,
                multiTenancy,
                Defaults.DEFAULT_TIMEOUT,
                localOperations,
                localEvents,
                localService,
                ServiceAsyncInterceptor.PASSTHROUGH,
                ServiceHandlerStrategy.PASSTHROUGH,
                Collections.emptySet());
        }
        
        @Bean
        public static ApplicationListener<ContextRefreshedEvent> listener(final MultiTenancy multiTenancy) {
            return event -> {
                multiTenancy.addTenant(MultiTenancy.SYSTEM_TENANT);
                multiTenancy.addTenant(TestTenants.DEFAULT);
            };
        }
        
    }
    
    @Autowired
    private ExampleServiceClientSpringImpl client;
    
    @Autowired
    private ExampleSpiServiceClientSpringImpl spiClient;
    
    @Autowired
    private ApplicationContext ctx;
    
    @Test
    public void testRequests_withSpringSetup() throws InterruptedException, ServiceException {
        client.doSomething();
        Thread.sleep(1000L);
    }
    
    @Test
    public void test_withMultipleStubs() throws Throwable {
        block(spiClient.doSomethingOnAll(1, 1000, 10, false));
    }
    
}
