package com.ixaris.commons.microservices.defaults.live;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.kafka.microservices.client.KafkaServiceClientSupport;
import com.ixaris.commons.kafka.multitenancy.KafkaConnectionHandler;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceDiscovery;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientFilterFactory;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.client.support.ServiceOperationDispatcher;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.zeromq.microservices.client.ZMQServiceOperationDispatcherRegistry;

public class ZMQKafkaServiceClientSupport extends ServiceClientSupport {
    
    private final ZMQServiceOperationDispatcherRegistry zmqServiceOperationDispatcherRegistry;
    private final KafkaServiceClientSupport kafkaServiceClientSupport;
    
    public ZMQKafkaServiceClientSupport(final ScheduledExecutorService executor,
                                        final MultiTenancy multiTenancy,
                                        final int defaultTimeout,
                                        final ServiceDiscovery serviceDiscovery,
                                        final ServiceAsyncInterceptor serviceAsyncInterceptor,
                                        final ServiceHandlerStrategy serviceHandlerStrategy,
                                        final Set<? extends ServiceClientFilterFactory> serviceFilterFactories,
                                        final LocalOperations localOperations,
                                        final KafkaConnectionHandler kafkaConnectionHandler) {
        super(executor,
            multiTenancy,
            defaultTimeout,
            endpointName -> () -> serviceDiscovery.getServiceKeys(endpointName),
            serviceAsyncInterceptor,
            serviceHandlerStrategy,
            serviceFilterFactories,
            localOperations);
        zmqServiceOperationDispatcherRegistry = new ZMQServiceOperationDispatcherRegistry(executor, serviceDiscovery);
        kafkaServiceClientSupport = new KafkaServiceClientSupport(executor, kafkaConnectionHandler);
    }
    
    @Override
    protected ServiceOperationDispatcher createOperationDispatcher(final String serviceName) {
        return zmqServiceOperationDispatcherRegistry.register(serviceName);
    }
    
    @Override
    protected void createEventHandler(final String serviceName,
                                      final String subscriberName,
                                      final ServicePathHolder path,
                                      final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain) {
        kafkaServiceClientSupport.createEventHandler(serviceName, subscriberName, path, filterChain);
    }
    
    @Override
    protected void destroyEventHandler(final String serviceName, final String subscriberName, final ServicePathHolder path) {
        kafkaServiceClientSupport.destroyEventHandler(serviceName, subscriberName, path);
    }
}
