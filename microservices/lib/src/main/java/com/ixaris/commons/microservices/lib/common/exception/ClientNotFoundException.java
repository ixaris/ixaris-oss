package com.ixaris.commons.microservices.lib.common.exception;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

public final class ClientNotFoundException extends ServiceException {
    
    public ClientNotFoundException() {
        super(ResponseStatusCode.CLIENT_NOT_FOUND, null);
    }
    
    public ClientNotFoundException(final String statusMessage) {
        super(ResponseStatusCode.CLIENT_NOT_FOUND, statusMessage);
    }
    
    public ClientNotFoundException(final Throwable cause) {
        super(ResponseStatusCode.CLIENT_NOT_FOUND, cause.getMessage(), cause);
    }
    
    public ClientNotFoundException(final String statusMessage, final Throwable cause) {
        super(ResponseStatusCode.CLIENT_NOT_FOUND, statusMessage, cause);
    }
    
}
