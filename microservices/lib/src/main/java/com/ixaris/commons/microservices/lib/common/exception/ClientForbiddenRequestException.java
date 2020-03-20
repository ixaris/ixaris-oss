package com.ixaris.commons.microservices.lib.common.exception;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

public final class ClientForbiddenRequestException extends ServiceException {
    
    public ClientForbiddenRequestException() {
        super(ResponseStatusCode.CLIENT_FORBIDDEN_REQUEST, null);
    }
    
    public ClientForbiddenRequestException(final String statusMessage) {
        super(ResponseStatusCode.CLIENT_FORBIDDEN_REQUEST, statusMessage);
    }
    
    public ClientForbiddenRequestException(final Throwable cause) {
        super(ResponseStatusCode.CLIENT_FORBIDDEN_REQUEST, cause.getMessage(), cause);
    }
    
    public ClientForbiddenRequestException(final String statusMessage, final Throwable cause) {
        super(ResponseStatusCode.CLIENT_FORBIDDEN_REQUEST, statusMessage, cause);
    }
    
}
