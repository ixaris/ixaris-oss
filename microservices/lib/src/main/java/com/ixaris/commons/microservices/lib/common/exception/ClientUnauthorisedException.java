package com.ixaris.commons.microservices.lib.common.exception;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

public final class ClientUnauthorisedException extends ServiceException {
    
    public ClientUnauthorisedException() {
        super(ResponseStatusCode.CLIENT_UNAUTHORISED, null);
    }
    
    public ClientUnauthorisedException(final String statusMessage) {
        super(ResponseStatusCode.CLIENT_UNAUTHORISED, statusMessage);
    }
    
    public ClientUnauthorisedException(final Throwable cause) {
        super(ResponseStatusCode.CLIENT_UNAUTHORISED, cause.getMessage(), cause);
    }
    
    public ClientUnauthorisedException(final String statusMessage, final Throwable cause) {
        super(ResponseStatusCode.CLIENT_UNAUTHORISED, statusMessage, cause);
    }
    
}
