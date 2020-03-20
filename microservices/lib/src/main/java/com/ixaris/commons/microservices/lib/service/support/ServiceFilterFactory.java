package com.ixaris.commons.microservices.lib.service.support;

import com.ixaris.commons.microservices.lib.common.ServiceEventFilter;
import com.ixaris.commons.microservices.lib.common.ServiceOperationFilter;
import com.ixaris.commons.misc.lib.object.Ordered;

public interface ServiceFilterFactory extends Ordered {
    
    default ServiceOperationFilter createOperationFilter(final String name, final String key) {
        return null;
    }
    
    default ServiceEventFilter createEventFilter(final String name, final String key) {
        return null;
    }
    
}
