package com.ixaris.commons.microservices.defaults.live;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.ixaris.commons.kafka.multitenancy.KafkaConnectionHandler;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceDiscovery;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientFilterFactory;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.service.discovery.ServiceRegistry;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceExceptionTranslator;
import com.ixaris.commons.microservices.lib.service.support.ServiceFilterFactory;
import com.ixaris.commons.microservices.lib.service.support.ServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.misc.lib.net.Localhost;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.zeromq.microservices.common.ZMQUrlHelper;

/**
 * A default spring configuration when using ZMQ and Kafka as your stack. This initiates a number of beans required for
 * managing this stack and
 * simplify client configuration when using spring.
 *
 * <p>A number of assumed beans are involved to inject different parameters per service/usage 1) serviceStubBasePackages
 * (string e.g.
 * "com.ixaris.balancesmonitor")
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
@SuppressWarnings("squid:S1200")
public class ZMQKafkaConfiguration {
    
    private static final int ZMQ_START_PORT = 9000;
    
    @Bean
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public static ZMQBean zmqBean() {
        return new ZMQBean();
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public ZMQUrlHelper zmqUrlHelper(@Value("${amq.start-port:" + ZMQ_START_PORT + "}") final int startPort) {
        return new ZMQUrlHelper(Localhost.HOSTNAME, startPort);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public ZMQKafkaServiceSupport serviceSupport(final ZMQBean zmqBean, // force instanciation
                                                 final ScheduledExecutorService executor,
                                                 final MultiTenancy multiTenancy,
                                                 final ServiceRegistry serviceRegistry,
                                                 final ServiceSecurityChecker serviceSecurityChecker,
                                                 final ServiceAsyncInterceptor serviceAsyncInterceptor,
                                                 final Optional<ServiceHandlerStrategy> serviceHandlerStrategy,
                                                 final Set<? extends ServiceExceptionTranslator<?>> serviceExceptionTranslators,
                                                 final Set<? extends ServiceFilterFactory> serviceFilterFactories,
                                                 final ServiceKeys serviceKeys,
                                                 final Optional<LocalOperations> localOperations,
                                                 final ZMQUrlHelper zmqUrlHelper,
                                                 final KafkaConnectionHandler kafkaConnectionHandler) {
        return new ZMQKafkaServiceSupport(executor,
            multiTenancy,
            serviceRegistry,
            serviceSecurityChecker,
            serviceAsyncInterceptor,
            serviceHandlerStrategy.orElse(ServiceHandlerStrategy.PASSTHROUGH),
            serviceExceptionTranslators,
            serviceFilterFactories,
            serviceKeys,
            localOperations.orElse(null),
            zmqUrlHelper,
            kafkaConnectionHandler);
    }
    
    @Bean
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public ZMQKafkaServiceClientSupport serviceClientSupport(final ZMQBean zmqBean, // force instanciation
                                                             final ScheduledExecutorService executor,
                                                             final MultiTenancy multiTenancy,
                                                             final Environment environment,
                                                             final ServiceDiscovery serviceDiscovery,
                                                             final ServiceAsyncInterceptor serviceAsyncInterceptor,
                                                             final Optional<ServiceHandlerStrategy> serviceHandlerStrategy,
                                                             final Set<? extends ServiceClientFilterFactory> processorFactories,
                                                             final Optional<LocalOperations> localOperations,
                                                             final KafkaConnectionHandler kafkaConnectionHandler) {
        return new ZMQKafkaServiceClientSupport(executor,
            multiTenancy,
            Defaults.DEFAULT_TIMEOUT,
            serviceDiscovery,
            serviceAsyncInterceptor,
            serviceHandlerStrategy.orElse(ServiceHandlerStrategy.PASSTHROUGH),
            processorFactories,
            localOperations.orElse(null),
            kafkaConnectionHandler);
    }
    
}
