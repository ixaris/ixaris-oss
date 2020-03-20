package com.ixaris.commons.microservices.web.swagger.operations;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;

/**
 * Response details, including parsed response. This can be mutated between different filters e.g. for tokenisation
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class SwaggerResponse {
    
    private final MessageLite response;
    private final ClientConflictException conflictException;
    private final Throwable throwable;
    
    public SwaggerResponse(final MessageLite response,
                           final ClientConflictException conflictException,
                           final Throwable throwable) {
        this.response = response;
        this.throwable = throwable;
        this.conflictException = conflictException;
    }
    
    public MessageLite getResponse() {
        return response;
    }
    
    public ClientConflictException getConflictException() {
        return conflictException;
    }
    
    public Throwable getThrowable() {
        return throwable;
    }
    
}
