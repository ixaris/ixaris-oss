package com.ixaris.commons.microservices.lib.client.proxy;

import java.lang.reflect.Constructor;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;

public final class StubResourceMethodInfo<RQ extends MessageLite, RS extends MessageLite, E extends MessageLite> {
    
    final String name;
    final Class<RQ> requestType;
    final Class<RS> responseType;
    final Class<E> conflictType;
    final Constructor<? extends ClientConflictException> conflictConstructor;
    
    StubResourceMethodInfo(final String name,
                           final Class<RQ> requestType,
                           final Class<RS> responseType,
                           final Class<E> conflictType,
                           final Constructor<? extends ClientConflictException> conflictConstructor) {
        this.name = name;
        this.requestType = requestType;
        this.responseType = responseType;
        this.conflictType = conflictType;
        this.conflictConstructor = conflictConstructor;
    }
    
    public String getName() {
        return name;
    }
    
    public Class<RQ> getRequestType() {
        return requestType;
    }
    
    public Class<RS> getResponseType() {
        return responseType;
    }
    
    public Class<E> getConflictType() {
        return conflictType;
    }
    
}
