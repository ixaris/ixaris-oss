package com.ixaris.commons.microservices.defaults.live;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.clustering.lib.service.ClusterDispatchFilterFactory;
import com.ixaris.commons.clustering.lib.service.ClusterHandleFilterFactory;
import com.ixaris.commons.clustering.lib.service.DefaultShardAllocationStrategy;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceDiscovery;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientFilterFactory;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.local.LocalEvents;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.local.LocalServiceClientSupport;
import com.ixaris.commons.microservices.lib.local.LocalServiceSupport;
import com.ixaris.commons.microservices.lib.service.discovery.ServiceRegistry;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceExceptionTranslator;
import com.ixaris.commons.microservices.lib.service.support.ServiceFilterFactory;
import com.ixaris.commons.microservices.lib.service.support.ServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;
import com.ixaris.commons.microservices.secrets.CertificateLoader;
import com.ixaris.commons.microservices.secrets.CertificateLoaderImpl;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.misc.lib.net.Localhost;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.TenantLifecycleListener;
import com.ixaris.commons.netty.clustering.NettyBean;
import com.ixaris.commons.netty.clustering.NettyClusterShardingFactory;
import com.ixaris.commons.zookeeper.clustering.LocalClusterRegistryHelperFactory;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterRegistryHelperFactory;
import com.ixaris.commons.zookeeper.microservices.ZookeeperServiceDiscovery;
import com.ixaris.commons.zookeeper.microservices.ZookeeperServiceDiscoveryConnection;
import com.ixaris.commons.zookeeper.microservices.ZookeeperServiceRegistry;
import com.ixaris.commons.zookeeper.multitenancy.TenantProvider;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClient;

@Import(ZMQKafkaConfiguration.class)
@ImportResource("classpath*:spring/*.xml")
@SuppressWarnings("squid:S1313")
public class LiveConfiguration {
    
    public static final int DEFAULT_NETTY_START_PORT = 9000;
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public static MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }
    
    @Bean
    public static CertificateLoader certificateLoader(@Value("${environment.name}") final String environment,
                                                      @Value("${spring.application.name}") final String serviceName,
                                                      @Value("${certificates.rootpath:}") final String certificateRootPath) {
        return new CertificateLoaderImpl(environment, serviceName, certificateRootPath);
    }
    
    //    @Bean
    //    @ExportMetricReader
    //    public static MetricReader metricReader(Optional<MetricRegistry> metricRegistry) {
    //        // Metrics are normally automatically exported by Spring...except if you use Dropwizard. Ergo, we need to setup
    //        // a reader/writer. This reader will periodically (by default, every 5 seconds) read metrics from the registry
    //        // to be exported by the metric writer, below
    //        return metricRegistry.map(MetricRegistryMetricReader::new).orElse(null);
    //    }
    //
    //    @Bean
    //    @ExportMetricWriter
    //    public static MetricWriter jmxMetricWriter(Optional<MBeanExporter> exporter) {
    //        // this metric writer will automatically export any metrics read by the metric reader above, as JMX beans.
    //        // this will allow external tools to read them in a standard way (eg. prometheus)
    //        return exporter
    //            .map(e -> {
    //                final JmxMetricWriter jmxMetricWriter = new JmxMetricWriter(e);
    //                // set the domain so that these beans will be namespaced to ixaris (and also, look better!)
    //                jmxMetricWriter.setDomain("com.ixaris.microservices");
    //                return jmxMetricWriter;
    //            })
    //            .orElse(null);
    //    }
    
    @Bean
    public LocalOperations localOperations(final ScheduledExecutorService executor) {
        return new LocalOperations(executor);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", havingValue = "true")
    public LocalEvents localEvents() {
        return new LocalEvents(AsyncExecutor.DEFAULT);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", havingValue = "true")
    public LocalServiceSupport localMicroservicesSupport(final MultiTenancy multiTenancy,
                                                         final LocalOperations localOperations,
                                                         final LocalEvents localEvents,
                                                         final ServiceRegistry serviceRegistry,
                                                         final ServiceSecurityChecker serviceSecurityChecker,
                                                         final ServiceAsyncInterceptor serviceAsyncInterceptor,
                                                         final Optional<ServiceHandlerStrategy> serviceHandlerStrategy,
                                                         final Set<? extends ServiceExceptionTranslator<?>> exceptionTranslators,
                                                         final Set<? extends ServiceFilterFactory> processorFactories,
                                                         final ServiceKeys serviceKeys) {
        return new LocalServiceSupport(multiTenancy,
            localOperations,
            localEvents,
            serviceRegistry,
            serviceSecurityChecker,
            serviceAsyncInterceptor,
            serviceHandlerStrategy.orElse(ServiceHandlerStrategy.PASSTHROUGH),
            exceptionTranslators,
            processorFactories,
            serviceKeys);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", havingValue = "true")
    public LocalServiceClientSupport localMicroservicesClientSupport(final MultiTenancy multiTenancy,
                                                                     final LocalOperations localOperations,
                                                                     final LocalEvents localEvents,
                                                                     final ServiceDiscovery serviceDiscovery,
                                                                     final ServiceAsyncInterceptor serviceAsyncInterceptor,
                                                                     final Optional<ServiceHandlerStrategy> serviceHandlerStrategy,
                                                                     final Set<? extends ServiceClientFilterFactory> processorFactories) {
        return new LocalServiceClientSupport(AsyncExecutor.DEFAULT,
            multiTenancy,
            Defaults.DEFAULT_TIMEOUT,
            localOperations,
            localEvents,
            serviceDiscovery,
            serviceAsyncInterceptor,
            serviceHandlerStrategy.orElse(ServiceHandlerStrategy.PASSTHROUGH),
            processorFactories);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", havingValue = "true")
    public LocalService localService(final Set<? extends ClusterDispatchFilterFactory> dispatchFilterFactories,
                                     final Set<? extends ClusterHandleFilterFactory> handleFilterFactories) {
        return new LocalService(dispatchFilterFactories, handleFilterFactories);
    }
    
    @Bean
    @ConditionalOnProperty(value = "cluster.enabled", matchIfMissing = true, havingValue = "false")
    public LocalClusterRegistryHelperFactory localClusterRegistryHelperFactory() {
        return new LocalClusterRegistryHelperFactory();
    }
    
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public NettyBean nettyBean(@Value("${netty.start-port:" + DEFAULT_NETTY_START_PORT + "}") final int startPort,
                               @Value("${netty.num-threads:1}") final int numThreads) {
        return new NettyBean(numThreads, Localhost.HOSTNAME, startPort);
    }
    
    @Bean
    @ConditionalOnProperty(value = "cluster.enabled", havingValue = "true")
    public NettyClusterShardingFactory nettyClusterShardingFactory(final NettyBean nettyBean, final ScheduledExecutorService executor) {
        return new NettyClusterShardingFactory(nettyBean, executor);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public static ZookeeperServiceDiscovery zookeeperServiceDiscovery(final ZookeeperServiceDiscoveryConnection zookeeperServiceDiscoveryConnection,
                                                                      final Executor executor) {
        return new ZookeeperServiceDiscovery(zookeeperServiceDiscoveryConnection, executor);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public static ZookeeperServiceRegistry zookeeperServiceRegistry(final ZookeeperServiceDiscoveryConnection zookeeperServiceDiscoveryConnection,
                                                                    final ZookeeperClusterRegistryHelperFactory clusterRegistryHelperFactory,
                                                                    final Executor executor,
                                                                    final Set<? extends ClusterDispatchFilterFactory> dispatchFilterFactories,
                                                                    final Set<? extends ClusterHandleFilterFactory> handleFilterFactories) {
        return new ZookeeperServiceRegistry(zookeeperServiceDiscoveryConnection,
            new DefaultShardAllocationStrategy(72),
            executor,
            clusterRegistryHelperFactory,
            dispatchFilterFactories,
            handleFilterFactories);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public static ZookeeperServiceDiscoveryConnection zookeeperServiceDiscoveryConnection(@Value("${spring.application.name}") final String serviceName,
                                                                                          final ZookeeperClient zookeeperClient) {
        return new ZookeeperServiceDiscoveryConnection(serviceName, zookeeperClient);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public static TenantProvider tenantProvider(final MultiTenancy multiTenancy,
                                                final ZookeeperClient zookeeperClient,
                                                @Value("${zookeeper.tenants.path:/tenants}") final String path) {
        return new TenantProvider(multiTenancy, zookeeperClient, path);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public ApplicationListener<ContextRefreshedEvent> startCluster(final ZookeeperServiceRegistry zookeeperServiceRegistry,
                                                                   final MultiTenancy multiTenancy,
                                                                   final TenantProvider tenantProvider) {
        return e -> {
            multiTenancy.addTenantLifecycleListener(new TenantLifecycleListener() {
                
                @Override
                public void onTenantActive(final String tenantId) {
                    if (tenantId.equals(MultiTenancy.SYSTEM_TENANT)) {
                        zookeeperServiceRegistry.start();
                        multiTenancy.removeTenantLifecycleListener(this);
                    }
                }
                
                @Override
                public void onTenantInactive(final String tenantId) {
                    // nothing to do
                }
                
            });
            multiTenancy.addTenant(MultiTenancy.SYSTEM_TENANT);
            tenantProvider.start();
        };
    }
    
    // (POSSIBLY) service admin console registration
    @Component
    @SuppressWarnings("squid:S1258")
    static class ManagementUrlInitializer {
        
        private static final String MANAGEMENT_URL_ATTR = "MANAGEMENT_URL";
        
        @Autowired
        private ServiceRegistry registry;
        
        @Autowired
        private Environment env;
        
        @Value("${management.port:${server.port:8080}}")
        private int port;
        
        @PostConstruct
        public void registerManagementUrl() {
            registry.mergeAttributes(Collections.singletonMap(MANAGEMENT_URL_ATTR, "http://" + host() + ":" + port));
        }
        
        private String host() {
            return Defaults.getOrDefault("advertisedUrl", env.getProperty("advertisedUrl"), Localhost.HOSTNAME);
        }
        
    }
    
}
