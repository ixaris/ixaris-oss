package com.ixaris.commons.microservices.lib.local;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceDiscovery;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientFilterFactory;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.client.support.ServiceOperationDispatcher;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

/**
 * Microservices support that initialises and handles microservices local channel that uses in-memory queues.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class LocalServiceClientSupport extends ServiceClientSupport {
    
    private final LocalOperations localOperations;
    private final LocalEvents localEvents;
    
    public LocalServiceClientSupport(final ScheduledExecutorService executor,
                                     final MultiTenancy multiTenancy,
                                     final int defaultTimeout,
                                     final LocalOperations localOperations,
                                     final LocalEvents localEvents,
                                     final ServiceDiscovery serviceDiscovery,
                                     final ServiceAsyncInterceptor serviceAsyncInterceptor,
                                     final ServiceHandlerStrategy serviceHandlerStrategy,
                                     final Set<? extends ServiceClientFilterFactory> filterFactories) {
        super(executor,
            multiTenancy,
            defaultTimeout,
            name -> () -> serviceDiscovery.getServiceKeys(name),
            serviceAsyncInterceptor,
            serviceHandlerStrategy,
            filterFactories,
            null);
        
        this.localOperations = localOperations;
        this.localEvents = localEvents;
    }
    
    @Override
    protected ServiceOperationDispatcher createOperationDispatcher(final String serviceName) {
        return localOperations.getOperationDispatcher(serviceName);
    }
    
    @Override
    protected void createEventHandler(final String serviceName,
                                      final String subscriberName,
                                      final ServicePathHolder path,
                                      final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain) {
        localEvents.createEventHandler(serviceName, subscriberName, path, filterChain);
    }
    
    @Override
    protected void destroyEventHandler(
                                       final String serviceName, final String subscriberName, final ServicePathHolder path) {
        localEvents.destroyEventHandler(serviceName, subscriberName, path);
    }
}
