package com.ixaris.commons.microservices.lib.service.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import com.google.protobuf.MessageLite;

public final class SkeletonResourceMethodInfo<RQ extends MessageLite, RS extends MessageLite, E extends MessageLite> {
    
    final ResourcePathParam[] pathParams;
    final Constructor<?> constructor;
    final Method method;
    final String security;
    final List<String> tags;
    final Class<RQ> requestType;
    final Class<RS> responseType;
    final Class<E> conflictType;
    
    SkeletonResourceMethodInfo(final ResourcePathParam[] pathParams,
                               final Constructor<?> constructor,
                               final Method method,
                               final String security,
                               final List<String> tags,
                               final Class<RQ> requestType,
                               final Class<RS> responseType,
                               final Class<E> conflictType) {
        
        this.pathParams = pathParams;
        this.constructor = constructor;
        this.method = method;
        this.security = security;
        this.tags = tags;
        this.requestType = requestType;
        this.responseType = responseType;
        this.conflictType = conflictType;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public String getSecurity() {
        return security;
    }
    
    public List<String> getTags() {
        return tags;
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
