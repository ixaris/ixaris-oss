package com.ixaris.commons.microservices.lib.client.support;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.microservices.lib.common.ServiceEventFilter;
import com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper;
import com.ixaris.commons.microservices.lib.common.ServiceOperationFilter;

public final class ServiceClientLoggingFilterFactory implements ServiceClientFilterFactory {
    
    public static final AsyncLocal<String> EVENT_CHANNEL = new AsyncLocal<>("event_channel");
    
    private final ServiceOperationFilter operationFilter;
    private final ServiceEventFilter eventFilter;
    
    public ServiceClientLoggingFilterFactory() {
        operationFilter = (in, next) -> ServiceLoggingHelper.logOperation(in, next, "Client", null);
        eventFilter = (in, next) -> ServiceLoggingHelper.logEvent(in, next, "Client", EVENT_CHANNEL::get);
    }
    
    @Override
    public ServiceOperationFilter createOperationFilter(final String name) {
        return operationFilter;
    }
    
    @Override
    public ServiceEventFilter createEventFilter(final String name) {
        return eventFilter;
    }
    
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
    
}
