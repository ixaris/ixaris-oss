package com.ixaris.commons.microservices.lib.common;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

public interface ServiceConstants {
    
    String GET_METHOD_NAME = "get";
    String WATCH_METHOD_NAME = "watch";
    
    enum ResponseStatusClass {
        OK,
        CLIENT_ERROR,
        SERVER_ERROR
    }
    
    static ResponseStatusClass resolveStatusClass(final ResponseStatusCode statusCode) {
        final int errorClass = statusCode.getNumber() / 100;
        if (errorClass == 2) {
            return ResponseStatusClass.OK;
        } else if (errorClass == 4) {
            return ResponseStatusClass.CLIENT_ERROR;
        } else if (errorClass == 5) {
            return ResponseStatusClass.SERVER_ERROR;
        } else {
            throw new IllegalStateException("Unresolvable status class for " + statusCode);
        }
    }
    
}
