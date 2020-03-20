package com.ixaris.commons.microservices.lib.local;

import java.util.Set;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
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

/**
 * Microservices support that initialises and handles microservices local channel that uses in-memory queues.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class LocalServiceSupport extends ServiceSupport {
    
    private final LocalOperations localOperations;
    private final LocalEvents localEvents;
    
    public LocalServiceSupport(final MultiTenancy multiTenancy,
                               final LocalOperations localOperations,
                               final LocalEvents localEvents,
                               final ServiceRegistry serviceRegistry,
                               final ServiceSecurityChecker serviceSecurityChecker,
                               final ServiceAsyncInterceptor serviceAsyncInterceptor,
                               final ServiceHandlerStrategy serviceHandlerStrategy,
                               final Set<? extends ServiceExceptionTranslator<?>> serviceExceptionTranslators,
                               final Set<? extends ServiceFilterFactory> serviceFilterFactories,
                               final ServiceKeys serviceKeys) {
        super(multiTenancy,
            serviceRegistry,
            serviceSecurityChecker,
            serviceAsyncInterceptor,
            serviceHandlerStrategy,
            serviceExceptionTranslators,
            serviceFilterFactories,
            serviceKeys,
            null);
        
        this.localOperations = localOperations;
        this.localEvents = localEvents;
    }
    
    @Override
    protected void createOperationHandler(final String serviceName,
                                          final String serviceKey,
                                          final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain) {
        localOperations.registerOperationHandler(serviceName, serviceKey, filterChain);
    }
    
    @Override
    public ServiceEventDispatcher createEventDispatcher(final String serviceName, final String serviceKey, final Set<ServicePathHolder> paths) {
        return localEvents.registerEventDispatcher(serviceName, serviceKey);
    }
    
    @Override
    protected void destroyOperationHandler(final String serviceName, final String serviceKey) {
        localOperations.deregisterOperationHandler(serviceName, serviceKey);
    }
    
    @Override
    protected void destroyEventDispatcher(final String serviceName, final String serviceKey) {
        localEvents.deregisterEventDispatcher(serviceName, serviceKey);
    }
    
}
