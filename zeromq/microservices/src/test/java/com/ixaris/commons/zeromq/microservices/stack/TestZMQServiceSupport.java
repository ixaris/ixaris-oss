package com.ixaris.commons.zeromq.microservices.stack;

import java.util.Collections;
import java.util.Set;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.support.DefaultServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceEventDispatcher;
import com.ixaris.commons.microservices.lib.service.support.ServiceLoggingFilterFactory;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;
import com.ixaris.commons.misc.lib.net.Localhost;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.zeromq.microservices.common.ZMQUrlHelper;
import com.ixaris.commons.zeromq.microservices.service.ZMQServiceOperationHandlerRegistry;

public class TestZMQServiceSupport extends ServiceSupport {
    
    private final ZMQUrlHelper urlHelper;
    private final ZMQServiceOperationHandlerRegistry operationHandlerRegistry;
    
    public TestZMQServiceSupport(final MultiTenancy multiTenancy,
                                 final int startPort,
                                 final LocalService localService,
                                 final TestServiceSecurityChecker testServiceSecurityChecker,
                                 final boolean logging) {
        super(multiTenancy,
            localService,
            testServiceSecurityChecker,
            ServiceAsyncInterceptor.PASSTHROUGH,
            ServiceHandlerStrategy.PASSTHROUGH,
            Collections.emptySet(),
            logging ? Collections.singleton(new ServiceLoggingFilterFactory()) : Collections.emptySet(),
            new DefaultServiceKeys(),
            null);
        urlHelper = new ZMQUrlHelper(Localhost.HOSTNAME, startPort);
        operationHandlerRegistry = new ZMQServiceOperationHandlerRegistry(AsyncExecutor.DEFAULT, urlHelper, localService);
    }
    
    public ZMQUrlHelper getUrlHelper() {
        return urlHelper;
    }
    
    @Override
    protected void createOperationHandler(final String serviceName,
                                          final String serviceKey,
                                          final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain) {
        operationHandlerRegistry.register(serviceName, serviceKey, filterChain);
    }
    
    @Override
    protected ServiceEventDispatcher createEventDispatcher(final String serviceName, final String serviceKey, final Set<ServicePathHolder> paths) {
        return eventEnvelope -> null;
    }
    
    @Override
    protected void destroyOperationHandler(final String serviceName, final String serviceKey) {
        operationHandlerRegistry.deregister(serviceName, serviceKey);
    }
    
    @Override
    protected void destroyEventDispatcher(final String serviceName, final String serviceKey) {}
    
}
