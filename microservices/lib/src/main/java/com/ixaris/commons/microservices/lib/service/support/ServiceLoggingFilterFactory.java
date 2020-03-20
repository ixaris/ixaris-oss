package com.ixaris.commons.microservices.lib.service.support;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.microservices.lib.common.ServiceEventFilter;
import com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper;
import com.ixaris.commons.microservices.lib.common.ServiceOperationFilter;

public final class ServiceLoggingFilterFactory implements ServiceFilterFactory {
    
    public static final AsyncLocal<String> OPERATION_CHANNEL = new AsyncLocal<>("operation_channel");
    
    private final ServiceOperationFilter operationFilter;
    private final ServiceEventFilter eventFilter;
    
    public ServiceLoggingFilterFactory() {
        operationFilter = (in, next) -> ServiceLoggingHelper.logOperation(in, next, "Service", OPERATION_CHANNEL::get);
        eventFilter = (in, next) -> ServiceLoggingHelper.logEvent(in, next, "Service", null);
    }
    
    @Override
    public ServiceOperationFilter createOperationFilter(final String name, final String key) {
        return operationFilter;
    }
    
    @Override
    public ServiceEventFilter createEventFilter(final String name, final String key) {
        return eventFilter;
    }
    
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
    
}
