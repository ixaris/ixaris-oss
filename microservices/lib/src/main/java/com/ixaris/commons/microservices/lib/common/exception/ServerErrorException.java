package com.ixaris.commons.microservices.lib.common.exception;

import com.google.protobuf.ByteString;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.protobuf.lib.MessageHelper;

import valid.Valid.MessageValidation;

public final class ServerErrorException extends ServiceException {
    
    private final MessageValidation messageValidation;
    
    public ServerErrorException() {
        super(ResponseStatusCode.SERVER_ERROR, null);
        this.messageValidation = null;
    }
    
    public ServerErrorException(final MessageValidation messageValidation) {
        super(ResponseStatusCode.SERVER_ERROR, MessageHelper.json(messageValidation));
        this.messageValidation = messageValidation;
    }
    
    public ServerErrorException(final String statusMessage) {
        this(statusMessage, (Throwable) null);
    }
    
    public ServerErrorException(final Throwable cause) {
        this(cause != null ? cause.getClass().getSimpleName() + " " + cause.getMessage() : null, cause);
    }
    
    public ServerErrorException(final String statusMessage, final Throwable cause) {
        super(ResponseStatusCode.SERVER_ERROR, statusMessage, cause);
        this.messageValidation = null;
    }
    
    public ServerErrorException(final String statusMessage, final MessageValidation messageValidation) {
        super(ResponseStatusCode.SERVER_ERROR, statusMessage);
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
