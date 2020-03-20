package com.ixaris.commons.microservices.lib.common.exception;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ServerTimeout;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.protobuf.lib.MessageHelper;

import valid.Valid.MessageValidation;

public abstract class ServiceException extends RuntimeException {
    
    private static final long serialVersionUID = -3956429792073987829L;
    
    public static ServiceException from(final ResponseStatusCode statusCode,
                                        final String statusMessage,
                                        final ByteString payload,
                                        final boolean json) {
        switch (statusCode) {
            case CLIENT_INVALID_REQUEST:
                try {
                    return new ClientInvalidRequestException(statusMessage,
                        payload == null ? null : MessageHelper.parse(MessageValidation.class, payload, json));
                } catch (final InvalidProtocolBufferException e) {
                    return new ServerErrorException(e);
                }
            case CLIENT_UNAUTHORISED:
                return new ClientUnauthorisedException(statusMessage);
            case CLIENT_FORBIDDEN_REQUEST:
                return new ClientForbiddenRequestException(statusMessage);
            case CLIENT_NOT_FOUND:
                return new ClientNotFoundException(statusMessage);
            case CLIENT_METHOD_NOT_ALLOWED:
                return new ClientMethodNotAllowedException(statusMessage);
            case CLIENT_TOO_MANY_REQUESTS:
                return new ClientTooManyRequestsException(statusMessage);
            case SERVER_ERROR:
                try {
                    return new ServerErrorException(statusMessage,
                        payload == null ? null : MessageHelper.parse(MessageValidation.class, payload, json));
                } catch (final InvalidProtocolBufferException e) {
                    return new ServerErrorException(e);
                }
            case SERVER_NOT_IMPLEMENTED:
                return new ServerNotImplementedException(statusMessage);
            case SERVER_UNAVAILABLE:
                return new ServerUnavailableException(statusMessage);
            case SERVER_TIMEOUT:
                try {
                    return new ServerTimeoutException(statusMessage,
                        payload == null ? null : MessageHelper.parse(ServerTimeout.class, payload, json));
                } catch (final InvalidProtocolBufferException e) {
                    return new ServerErrorException(e);
                }
            default:
                throw new UnsupportedOperationException();
        }
    }
    
    protected final ResponseStatusCode statusCode;
    
    ServiceException(final ResponseStatusCode statusCode, final String statusMessage) {
        super(statusMessage);
        this.statusCode = statusCode;
    }
    
    ServiceException(final ResponseStatusCode statusCode, final String statusMessage, final Throwable cause) {
        super(statusMessage, cause);
        this.statusCode = statusCode;
    }
    
    public ResponseStatusCode getStatusCode() {
        return statusCode;
    }
    
    public ByteString getPayload(final boolean json) {
        return null;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> statusCode == other.statusCode);
    }
    
    @Override
    public int hashCode() {
        return statusCode != null ? statusCode.hashCode() : 0;
    }
    
}
