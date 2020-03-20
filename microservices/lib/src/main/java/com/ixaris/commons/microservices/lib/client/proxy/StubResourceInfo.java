package com.ixaris.commons.microservices.lib.client.proxy;

import java.util.Map;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.misc.lib.object.ProxyFactory;

public final class StubResourceInfo {
    
    final ProxyFactory proxyFactory;
    final Class<?> resourceType;
    final ServicePathHolder path;
    final String paramName;
    final StubResourceInfo paramInfo;
    final Map<String, StubResourceInfo> resourceMap;
    final Map<String, StubResourceMethodInfo<?, ?, ?>> methodMap;
    final Class<? extends MessageLite> eventType;
    
    StubResourceInfo(final ProxyFactory proxyFactory,
                     final Class<?> resourceType,
                     final ServicePathHolder path,
                     final String paramName,
                     final StubResourceInfo paramInfo,
                     final Map<String, StubResourceInfo> resourceMap,
                     final Map<String, StubResourceMethodInfo<?, ?, ?>> methodMap,
                     final Class<? extends MessageLite> eventType) {
        this.proxyFactory = proxyFactory;
        this.resourceType = resourceType;
        this.path = path;
        this.paramName = paramName;
        this.paramInfo = paramInfo;
        this.resourceMap = resourceMap;
        this.methodMap = methodMap;
        this.eventType = eventType;
    }
    
    public ServicePathHolder getPath() {
        return path;
    }
    
    public StubResourceInfo getParamInfo() {
        return paramInfo;
    }
    
    public Map<String, StubResourceInfo> getResourceMap() {
        return resourceMap;
    }
    
    public Map<String, StubResourceMethodInfo<?, ?, ?>> getMethodMap() {
        return methodMap;
    }
    
}
