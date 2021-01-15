package com.ixaris.commons.zeromq.microservices.stack;

import java.util.Collections;
import java.util.Set;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientLoggingFilterFactory;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.client.support.ServiceOperationDispatcher;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.zeromq.microservices.client.ZMQServiceOperationDispatcherRegistry;

public class TestZMQServiceClientSupport extends ServiceClientSupport {
    
    private final ZMQServiceOperationDispatcherRegistry operationDispatcherRegistry;
    
    public TestZMQServiceClientSupport(final MultiTenancy multiTenancy, final LocalService localService, final boolean logging) {
        super(AsyncExecutor.DEFAULT,
            multiTenancy,
            5000,
            name -> () -> localService.getServiceKeys(name),
            ServiceAsyncInterceptor.PASSTHROUGH,
            ServiceHandlerStrategy.PASSTHROUGH,
            logging ? Collections.singleton(new ServiceClientLoggingFilterFactory()) : Collections.emptySet(),
            null);
        operationDispatcherRegistry = new ZMQServiceOperationDispatcherRegistry(AsyncExecutor.DEFAULT, localService);
    }
    
    @Override
    protected ServiceOperationDispatcher createOperationDispatcher(final String serviceName) {
        return operationDispatcherRegistry.register(serviceName);
    }
    
    @Override
    protected void createEventHandler(final String serviceName,
                                      final String subscriberName,
                                      final ServicePathHolder path,
                                      final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain) {}
    
    @Override
    protected void destroyEventHandler(final String serviceName, final String subscriberName, final ServicePathHolder path) {}
    
}
