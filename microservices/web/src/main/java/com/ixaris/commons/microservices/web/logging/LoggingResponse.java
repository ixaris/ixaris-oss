package com.ixaris.commons.microservices.web.logging;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

/**
 * All the response-related data used for logging
 *
 * @author benjie.gatt
 */
public class LoggingResponse {
    
    private final MessageLite response;
    private final String sanitisedPayload;
    private final ResponseStatusCode statusCode;
    
    private LoggingResponse(final MessageLite response, final ResponseStatusCode statusCode) {
        this.response = response;
        this.sanitisedPayload = null;
        this.statusCode = statusCode;
    }
    
    private LoggingResponse(final String sanitisedPayload, final ResponseStatusCode statusCode) {
        this.response = null;
        this.sanitisedPayload = sanitisedPayload;
        this.statusCode = statusCode;
    }
    
    private LoggingResponse(final ResponseStatusCode statusCode) {
        this.response = null;
        this.sanitisedPayload = null;
        this.statusCode = statusCode;
    }
    
    public static LoggingResponse fromEmptyResponse(final ResponseStatusCode responseStatusCode) {
        return new LoggingResponse(responseStatusCode);
    }
    
    public static LoggingResponse fromUnsanitisedMessage(
                                                         final MessageLite responseMessage, final ResponseStatusCode responseStatusCode) {
        return new LoggingResponse(responseMessage, responseStatusCode);
    }
    
    public static LoggingResponse fromSanitisedPayload(
                                                       final String sanitisedPayload, final ResponseStatusCode responseStatusCode) {
        return new LoggingResponse(sanitisedPayload, responseStatusCode);
    }
    
    public MessageLite getResponse() {
        return response;
    }
    
    public String getSanitisedPayload() {
        return sanitisedPayload;
    }
    
    public ResponseStatusCode getStatusCode() {
        return statusCode;
    }
    
}
