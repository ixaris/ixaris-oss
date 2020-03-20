package com.ixaris.commons.microservices.lib.service.support;

import com.ixaris.commons.microservices.lib.common.exception.ServiceException;

public interface ServiceExceptionTranslator<T extends Exception> {
    
    ServiceException translate(final T exception);
    
}
