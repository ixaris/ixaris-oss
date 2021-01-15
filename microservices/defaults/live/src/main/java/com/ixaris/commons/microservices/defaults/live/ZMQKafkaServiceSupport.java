package com.ixaris.commons.microservices.defaults.live;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.kafka.microservices.service.KafkaServiceSupport;
import com.ixaris.commons.kafka.multitenancy.KafkaConnectionHandler;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.discovery.ServiceRegistry;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceEventDispatcher;
import com.ixaris.commons.microservices.lib.service.support.ServiceExceptionTranslator;
import com.ixaris.commons.microservices.lib.service.support.ServiceFilterFactory;
import com.ixaris.commons.microservices.lib.service.support.ServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.zeromq.microservices.common.ZMQUrlHelper;
import com.ixaris.commons.zeromq.microservices.service.ZMQServiceOperationHandlerRegistry;

public class ZMQKafkaServiceSupport extends ServiceSupport {
    
    private final ZMQServiceOperationHandlerRegistry zmqServiceOperationHandlerRegistry;
    private final KafkaServiceSupport kafkaServiceSupport;
    
    public ZMQKafkaServiceSupport(final ScheduledExecutorService executor,
                                  final MultiTenancy multiTenancy,
                                  final ServiceRegistry serviceRegistry,
                                  final ServiceSecurityChecker serviceSecurityChecker,
                                  final ServiceAsyncInterceptor serviceAsyncInterceptor,
                                  final ServiceHandlerStrategy serviceHandlerStrategy,
                                  final Set<? extends ServiceExceptionTranslator<?>> serviceExceptionTranslators,
                                  final Set<? extends ServiceFilterFactory> serviceFilterFactories,
                                  final ServiceKeys serviceKeys,
                                  final LocalOperations localOperations,
                                  final ZMQUrlHelper zmqUrlHelper,
                                  final KafkaConnectionHandler kafkaConnectionHandler) {
        super(multiTenancy,
            serviceRegistry,
            serviceSecurityChecker,
            serviceAsyncInterceptor,
            serviceHandlerStrategy,
            serviceExceptionTranslators,
            serviceFilterFactories,
            serviceKeys,
            localOperations);
        zmqServiceOperationHandlerRegistry = new ZMQServiceOperationHandlerRegistry(executor, zmqUrlHelper, serviceRegistry);
        kafkaServiceSupport = new KafkaServiceSupport(kafkaConnectionHandler);
    }
    
    @Override
    protected void createOperationHandler(final String serviceName,
                                          final String serviceKey,
                                          final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain) {
        zmqServiceOperationHandlerRegistry.register(serviceName, serviceKey, filterChain);
    }
    
    @Override
    protected ServiceEventDispatcher createEventDispatcher(final String serviceName, final String serviceKey, final Set<ServicePathHolder> paths) {
        return kafkaServiceSupport.createEventsDispatcher(serviceName, paths);
    }
    
    @Override
    protected void destroyOperationHandler(final String serviceName, final String serviceKey) {
        zmqServiceOperationHandlerRegistry.deregister(serviceName, serviceKey);
    }
    
    @Override
    protected void destroyEventDispatcher(final String serviceName, final String serviceKey) {
        // kafka does not require this
    }
    
}
