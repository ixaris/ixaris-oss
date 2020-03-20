package com.ixaris.commons.microservices.lib.common.exception;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.protobuf.lib.MessageHelper;

public abstract class ClientConflictException extends Exception {
    
    private final transient MessageLite conflict;
    
    public ClientConflictException(final MessageLite conflict) {
        super();
        if (conflict == null) {
            throw new IllegalArgumentException("conflict is null");
        }
        this.conflict = conflict;
    }
    
    public ClientConflictException(final MessageLite conflict, final Throwable cause) {
        super(cause);
        if (conflict == null) {
            throw new IllegalArgumentException("conflict is null");
        }
        this.conflict = conflict;
    }
    
    public ByteString getPayload(final boolean json) {
        return conflict != null ? MessageHelper.bytes(conflict, json) : null;
    }
    
    public MessageLite getConflict() {
        return conflict;
    }
    
    @Override
    public String getMessage() {
        return conflict.toString();
    }
}
