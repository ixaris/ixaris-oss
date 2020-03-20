package com.ixaris.commons.microservices.lib.client.support;

import com.ixaris.commons.microservices.lib.common.ServiceEventFilter;
import com.ixaris.commons.microservices.lib.common.ServiceOperationFilter;
import com.ixaris.commons.misc.lib.object.Ordered;

public interface ServiceClientFilterFactory extends Ordered {
    
    default ServiceOperationFilter createOperationFilter(final String name) {
        return null;
    }
    
    default ServiceEventFilter createEventFilter(final String name) {
        return null;
    }
    
}
