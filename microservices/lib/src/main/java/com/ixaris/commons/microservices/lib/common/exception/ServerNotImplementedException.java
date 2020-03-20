package com.ixaris.commons.microservices.lib.common.exception;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

public final class ServerNotImplementedException extends ServiceException {
    
    public ServerNotImplementedException() {
        super(ResponseStatusCode.SERVER_NOT_IMPLEMENTED, null);
    }
    
    public ServerNotImplementedException(final String statusMessage) {
        super(ResponseStatusCode.SERVER_NOT_IMPLEMENTED, statusMessage);
    }
    
    public ServerNotImplementedException(final Throwable cause) {
        super(ResponseStatusCode.SERVER_NOT_IMPLEMENTED, cause.getMessage(), cause);
    }
    
    public ServerNotImplementedException(final String statusMessage, final Throwable cause) {
        super(ResponseStatusCode.SERVER_NOT_IMPLEMENTED, statusMessage, cause);
    }
    
}
