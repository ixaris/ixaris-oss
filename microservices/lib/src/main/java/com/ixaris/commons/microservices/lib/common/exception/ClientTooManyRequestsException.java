package com.ixaris.commons.microservices.lib.common.exception;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

public final class ClientTooManyRequestsException extends ServiceException {
    
    public ClientTooManyRequestsException() {
        super(ResponseStatusCode.CLIENT_TOO_MANY_REQUESTS, null);
    }
    
    public ClientTooManyRequestsException(final String statusMessage) {
        super(ResponseStatusCode.CLIENT_TOO_MANY_REQUESTS, statusMessage);
    }
    
    public ClientTooManyRequestsException(final Throwable cause) {
        super(ResponseStatusCode.CLIENT_TOO_MANY_REQUESTS, cause.getMessage(), cause);
    }
    
    public ClientTooManyRequestsException(final String statusMessage, final Throwable cause) {
        super(ResponseStatusCode.CLIENT_TOO_MANY_REQUESTS, statusMessage, cause);
    }
    
}
