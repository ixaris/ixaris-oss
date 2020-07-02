package com.ixaris.commons.jooq.microservices;

import org.springframework.stereotype.Component;

import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.service.support.ServiceExceptionTranslator;
import com.ixaris.commons.persistence.lib.exception.DuplicateEntryException;

@Component
public class DuplicateEntryExceptionTranslator implements ServiceExceptionTranslator<DuplicateEntryException> {
    
    @Override
    public ServiceException translate(final DuplicateEntryException e) {
        return new ClientInvalidRequestException(e);
    }
    
}
