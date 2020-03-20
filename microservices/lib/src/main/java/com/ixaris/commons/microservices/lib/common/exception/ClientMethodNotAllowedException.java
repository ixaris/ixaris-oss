package com.ixaris.commons.microservices.lib.common.exception;

import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

public final class ClientMethodNotAllowedException extends ServiceException {
    
    public ClientMethodNotAllowedException() {
        super(ResponseStatusCode.CLIENT_METHOD_NOT_ALLOWED, null);
    }
    
    public ClientMethodNotAllowedException(final String statusMessage) {
        super(ResponseStatusCode.CLIENT_METHOD_NOT_ALLOWED, statusMessage);
    }
    
    public ClientMethodNotAllowedException(final Throwable cause) {
        super(ResponseStatusCode.CLIENT_METHOD_NOT_ALLOWED, cause.getMessage(), cause);
    }
    
    public ClientMethodNotAllowedException(final String statusMessage, final Throwable cause) {
        super(ResponseStatusCode.CLIENT_METHOD_NOT_ALLOWED, statusMessage, cause);
    }
    
    public ClientMethodNotAllowedException(final RequestEnvelope requestEnvelope) {
        this(String.format("Unsupported Operation [%s] on path [%s]!", requestEnvelope.getMethod(), ServicePathHolder.of(requestEnvelope.getPathList())));
    }
}
