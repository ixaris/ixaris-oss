package com.ixaris.commons.microservices.lib.common.exception;

import com.google.protobuf.ByteString;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.protobuf.lib.MessageHelper;

import valid.Valid.MessageValidation;

@SuppressWarnings("squid:S2972")
public final class ClientInvalidRequestException extends ServiceException {
    
    private final MessageValidation messageValidation;
    
    public ClientInvalidRequestException() {
        super(ResponseStatusCode.CLIENT_INVALID_REQUEST, null);
        this.messageValidation = null;
    }
    
    public ClientInvalidRequestException(final MessageValidation messageValidation) {
        this(MessageHelper.json(messageValidation), messageValidation);
    }
    
    public ClientInvalidRequestException(final String statusMessage) {
        super(ResponseStatusCode.CLIENT_INVALID_REQUEST, statusMessage);
        this.messageValidation = null;
    }
    
    public ClientInvalidRequestException(final Throwable cause) {
        this(cause.getMessage(), cause);
    }
    
    public ClientInvalidRequestException(final String statusMessage, final Throwable cause) {
        super(ResponseStatusCode.CLIENT_INVALID_REQUEST, statusMessage, cause);
        this.messageValidation = null;
    }
    
    public ClientInvalidRequestException(final String statusMessage, final MessageValidation messageValidation) {
        super(ResponseStatusCode.CLIENT_INVALID_REQUEST, statusMessage);
        this.messageValidation = messageValidation;
    }
    
    public ClientInvalidRequestException(final String statusMessage, final MessageValidation messageValidation, final Throwable cause) {
        super(ResponseStatusCode.CLIENT_INVALID_REQUEST, statusMessage, cause);
        this.messageValidation = messageValidation;
    }
    
    @Override
    public ByteString getPayload(final boolean json) {
        return messageValidation != null ? MessageHelper.bytes(messageValidation, json) : null;
    }
    
    public MessageValidation getMessageValidation() {
        return messageValidation;
    }
    
}
