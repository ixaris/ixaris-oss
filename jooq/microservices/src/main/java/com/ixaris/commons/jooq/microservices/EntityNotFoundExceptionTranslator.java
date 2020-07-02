package com.ixaris.commons.jooq.microservices;

import org.springframework.stereotype.Component;

import com.ixaris.commons.microservices.lib.common.exception.ClientNotFoundException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.service.support.ServiceExceptionTranslator;
import com.ixaris.commons.persistence.lib.exception.EntityNotFoundException;

@Component
public class EntityNotFoundExceptionTranslator implements ServiceExceptionTranslator<EntityNotFoundException> {
    
    @Override
    public ServiceException translate(final EntityNotFoundException e) {
        return new ClientNotFoundException(e);
    }
    
}
