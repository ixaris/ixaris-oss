package com.ixaris.commons.microservices.web;

import java.util.Collections;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceDiscovery;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.local.LocalEvents;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.local.LocalServiceClientSupport;
import com.ixaris.commons.microservices.lib.local.LocalServiceSupport;
import com.ixaris.commons.microservices.lib.service.discovery.ServiceRegistry;
import com.ixaris.commons.microservices.lib.service.support.DefaultServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;
import com.ixaris.commons.microservices.test.mocks.SkeletonResourceMock;
import com.ixaris.commons.microservices.web.service1.Service1.ExampleContext;
import com.ixaris.commons.microservices.web.service1.service.Service1Skeleton;
import com.ixaris.commons.microservices.web.service2.service.Service2Skeleton;
import com.ixaris.commons.microservices.web.service2alt.service.Service2AltSkeleton;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

@Configuration
public class TestConfiguration {
    
    @Bean
    public LocalService localService() {
        return new LocalService(Collections.emptySet(), Collections.emptySet());
    }
    
    @Bean
    public ServiceSecurityChecker serviceSecurityChecker() {
        return (header, security, tags) -> {
            if ((security == null) || !security.equals("UNSECURED")) {
                return !((ExampleContext) header.getContext()).getAuthToken().isEmpty();
            } else {
                return true;
            }
        };
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
                                                         final ServiceRegistry serviceRegistry,
                                                         final ServiceSecurityChecker serviceSecurityChecker,
                                                         final ServiceKeys serviceKeys) {
        return new LocalServiceSupport(multiTenancy,
            localOperations,
            localEvents,
            serviceRegistry,
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
                                                                     final ServiceDiscovery serviceDiscovery) {
        return new LocalServiceClientSupport(AsyncExecutor.DEFAULT,
            multiTenancy,
            Defaults.DEFAULT_TIMEOUT,
            localOperations,
            localEvents,
            serviceDiscovery,
            ServiceAsyncInterceptor.PASSTHROUGH,
            ServiceHandlerStrategy.PASSTHROUGH,
            Collections.emptySet());
    }
    
    @Bean
    public ApplicationListener<ContextRefreshedEvent> addTenant(final MultiTenancy multiTenancy) {
        return event -> multiTenancy.addTenant(TestTenants.DEFAULT);
    }
    
    @Bean
    public Service1Skeleton mockService1() {
        return SkeletonResourceMock.mock(Service1Skeleton.class);
    }
    
    @Bean
    public Service2Skeleton mockService2() {
        return SkeletonResourceMock.mock(Service2Skeleton.class);
    }
    
    @Bean
    public Service2AltSkeleton mockService2Alt() {
        return SkeletonResourceMock.mock(Service2AltSkeleton.class);
    }
    
}
