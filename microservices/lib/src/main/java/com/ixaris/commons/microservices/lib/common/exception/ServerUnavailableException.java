package com.ixaris.commons.microservices.lib.common.exception;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

public final class ServerUnavailableException extends ServiceException {
    
    public ServerUnavailableException() {
        super(ResponseStatusCode.SERVER_UNAVAILABLE, null);
    }
    
    public ServerUnavailableException(final String statusMessage) {
        super(ResponseStatusCode.SERVER_UNAVAILABLE, statusMessage);
    }
    
    public ServerUnavailableException(final Throwable cause) {
        super(ResponseStatusCode.SERVER_UNAVAILABLE, cause.getMessage(), cause);
    }
    
    public ServerUnavailableException(final String statusMessage, final Throwable cause) {
        super(ResponseStatusCode.SERVER_UNAVAILABLE, statusMessage, cause);
    }
    
}
