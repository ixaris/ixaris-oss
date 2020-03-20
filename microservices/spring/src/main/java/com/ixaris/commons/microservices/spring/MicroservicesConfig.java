package com.ixaris.commons.microservices.spring;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.ixaris.commons.async.lib.executor.AsyncScheduledExecutorServiceWrapper;
import com.ixaris.commons.async.lib.thread.NamedThreadFactory;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientLoggingFilterFactory;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.client.support.ServiceEventClusterRouteHandlerFactory;
import com.ixaris.commons.microservices.lib.service.support.DefaultServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceLoggingFilterFactory;
import com.ixaris.commons.microservices.lib.service.support.ServiceOperationClusterRouteHandlerFactory;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;
import com.ixaris.commons.multitenancy.lib.async.ExecutorMultiTenantAtLeastOnceProcessorFactory;
import com.ixaris.commons.multitenancy.lib.async.MultiTenantAtLeastOnceProcessorFactory;

@Configuration
public class MicroservicesConfig {
    
    @Bean
    public DefaultServiceKeys defaultServiceKeys() {
        return new DefaultServiceKeys();
    }
    
    @Bean
    public ServiceOperationClusterRouteHandlerFactory serviceOperationClusterRouteHandlerFactory(final ClusterRegistry clusterRegistry, final ServiceSupport serviceSupport) {
        return new ServiceOperationClusterRouteHandlerFactory(clusterRegistry, serviceSupport);
    }
    
    @Bean
    public ServiceEventClusterRouteHandlerFactory serviceEventClusterRouteHandlerFactory(final ClusterRegistry clusterRegistry, final ServiceClientSupport serviceClientSupport) {
        return new ServiceEventClusterRouteHandlerFactory(clusterRegistry, serviceClientSupport);
    }
    
    @Bean
    @Primary
    public ScheduledExecutorService scheduledExecutorService(@Value("${async.allow-join:true}") final boolean allowJoin) {
        return new AsyncScheduledExecutorServiceWrapper<>(allowJoin,
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new NamedThreadFactory("BaseExecutor-")));
    }
    
    @Bean
    public MultiTenantAtLeastOnceProcessorFactory multiTenantAtLeastOnceProcessorFactory(final ScheduledExecutorService executor) {
        return new ExecutorMultiTenantAtLeastOnceProcessorFactory(executor);
    }
    
    @Bean
    public ServiceLoggingFilterFactory serviceMonitoringProcessorFactory() {
        return new ServiceLoggingFilterFactory();
    }
    
    @Bean
    public ServiceClientLoggingFilterFactory serviceClientMonitoringProcessorFactory() {
        return new ServiceClientLoggingFilterFactory();
    }
    
    @Bean
    public static ServiceSkeletonBeanPostProcessor serviceSkeletonBeanPostProcessor(final ServiceSupport serviceSupport) {
        return new ServiceSkeletonBeanPostProcessor(serviceSupport);
    }
    
    @Bean
    public static ServiceStubAndPublisherBeanFactoryPostProcessor serviceStubScanBeanFactoryPostProcessor(final ServiceSupport serviceSupport,
                                                                                                          final ServiceClientSupport serviceClientSupport) {
        return new ServiceStubAndPublisherBeanFactoryPostProcessor(serviceSupport, serviceClientSupport);
    }
    
}
