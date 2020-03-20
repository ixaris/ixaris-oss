package com.ixaris.commons.microservices.spring;

import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import com.ixaris.commons.microservices.lib.common.exception.ServerTimeoutException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.service.support.ServiceExceptionTranslator;

@Component
public class TimeoutExceptionTranslator implements ServiceExceptionTranslator<TimeoutException> {
    
    @Override
    public ServiceException translate(final TimeoutException e) {
        return new ServerTimeoutException(e);
    }
    
}
