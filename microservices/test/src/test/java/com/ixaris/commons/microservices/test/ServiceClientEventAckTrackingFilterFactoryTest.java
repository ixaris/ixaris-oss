package com.ixaris.commons.microservices.test;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.test.CompletionStageAssert;
import com.ixaris.commons.microservices.lib.client.ServiceEventSubscription;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientFilterFactory;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.exception.ServerErrorException;
import com.ixaris.commons.microservices.lib.local.LocalEvents;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.local.LocalServiceClientSupport;
import com.ixaris.commons.microservices.lib.local.LocalServiceSupport;
import com.ixaris.commons.microservices.lib.service.support.DefaultServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceExceptionTranslator;
import com.ixaris.commons.microservices.lib.service.support.ServiceFilterFactory;
import com.ixaris.commons.microservices.lib.service.support.ServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;
import com.ixaris.commons.microservices.test.example.Example.ExampleContext;
import com.ixaris.commons.microservices.test.example.ExampleSkeletonImpl;
import com.ixaris.commons.microservices.test.example.client.ExampleStub;
import com.ixaris.commons.microservices.test.example.service.ExampleSkeleton;
import com.ixaris.commons.microservices.test.localstack.TestServiceSecurityChecker;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ServiceClientEventAckTrackingFilterFactoryTest {
    
    @Configuration
    @ComponentScan({ "com.ixaris.commons.microservices.test", "com.ixaris.commons.microservices.spring", "com.ixaris.commons.multitenancy.spring" })
    static class ContextConfiguration {
        
        @Bean
        public LocalService localServiceDiscovery() {
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
        public LocalServiceSupport localServiceSupport(final MultiTenancy multiTenancy,
                                                       final LocalOperations localOperations,
                                                       final LocalEvents localEvents,
                                                       final LocalService localService,
                                                       final ServiceSecurityChecker serviceSecurityChecker,
                                                       final Set<? extends ServiceExceptionTranslator<?>> exceptionTranslators,
                                                       final Set<? extends ServiceFilterFactory> processorFactories,
                                                       final ServiceKeys serviceKeys) {
            return new LocalServiceSupport(multiTenancy,
                localOperations,
                localEvents,
                localService,
                serviceSecurityChecker,
                ServiceAsyncInterceptor.PASSTHROUGH,
                ServiceHandlerStrategy.PASSTHROUGH,
                exceptionTranslators,
                processorFactories,
                serviceKeys);
        }
        
        @Bean
        public LocalServiceClientSupport localServiceClientSupport(final MultiTenancy multiTenancy,
                                                                   final LocalOperations localOperations,
                                                                   final LocalEvents localEvents,
                                                                   final LocalService localService,
                                                                   final Set<? extends ServiceClientFilterFactory> processorFactories) {
            return new LocalServiceClientSupport(AsyncExecutor.DEFAULT,
                multiTenancy,
                Defaults.DEFAULT_TIMEOUT,
                localOperations,
                localEvents,
                localService,
                ServiceAsyncInterceptor.PASSTHROUGH,
                ServiceHandlerStrategy.PASSTHROUGH,
                processorFactories);
        }
        
        @Bean
        public ServiceClientEventAckTrackingFilterFactory serviceClientEventAckTrackingFilterFactory() {
            return new ServiceClientEventAckTrackingFilterFactory();
        }
        
        @Bean
        public static ApplicationListener<ContextRefreshedEvent> listener(final MultiTenancy multiTenancy) {
            return event -> multiTenancy.addTenant(TestTenants.DEFAULT);
        }
        
    }
    
    @Autowired
    private ExampleStub exampleStub;
    
    @Autowired
    private ExampleSkeleton.Watch eventPublisher;
    
    @Autowired
    private ExampleSkeletonImpl service;
    
    @Autowired
    private ServiceClientEventAckTrackingFilterFactory clientEventAckTracking;
    
    private ServiceEventSubscription subscription;
    
    @Test
    public void testSuccess() {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            final ServiceEventSubscription subscription = exampleStub.watch((header, event) -> result());
            try {
                final ServiceEventHeader<ExampleContext> header = ServiceEventHeader.newBuilder(ExampleContext
                    .getDefaultInstance())
                    .build();
                final CompletionStage<Void> eventFuture = clientEventAckTracking.register(header, eventPublisher);
                service.publish(header);
                CompletionStageAssert.assertThat(eventFuture).await(Defaults.DEFAULT_TIMEOUT).isFulfilled();
            } finally {
                subscription.cancel();
            }
        });
    }
    
    @Test
    public void testFailure() {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            final ServiceEventSubscription subscription = exampleStub.watch((header, event) -> {
                throw new IllegalStateException();
            });
            try {
                final ServiceEventHeader<ExampleContext> header = ServiceEventHeader.newBuilder(ExampleContext
                    .getDefaultInstance())
                    .build();
                final CompletionStage<Void> eventFuture = clientEventAckTracking.register(header, eventPublisher);
                service.publish(header);
                CompletionStageAssert
                    .assertThat(eventFuture)
                    .await(Defaults.DEFAULT_TIMEOUT)
                    .isRejectedWith(ServerErrorException.class);
            } finally {
                subscription.cancel();
            }
        });
    }
    
}
