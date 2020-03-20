package com.ixaris.commons.microservices.lib.service.proxy;

import java.lang.reflect.Method;
import java.util.Map;

public final class SkeletonResourceInfo {
    
    final Method resourceMethod;
    final PathParamType paramType;
    final SkeletonResourceInfo paramInfo;
    final Map<String, SkeletonResourceInfo> resourceMap;
    final Map<String, SkeletonResourceMethodInfo<?, ?, ?>> methodMap;
    
    SkeletonResourceInfo(final Method resourceMethod,
                         final Map<String, SkeletonResourceMethodInfo<?, ?, ?>> methodMap,
                         final PathParamType paramType,
                         final SkeletonResourceInfo paramInfo,
                         final Map<String, SkeletonResourceInfo> resourceMap) {
        this.resourceMethod = resourceMethod;
        this.methodMap = methodMap;
        this.paramType = paramType;
        this.paramInfo = paramInfo;
        this.resourceMap = resourceMap;
    }
    
    public Map<String, SkeletonResourceMethodInfo<?, ?, ?>> getMethodMap() {
        return methodMap;
    }
    
    public PathParamType getParamType() {
        return paramType;
    }
    
    public SkeletonResourceInfo getParamInfo() {
        return paramInfo;
    }
    
    public Map<String, SkeletonResourceInfo> getResourceMap() {
        return resourceMap;
    }
    
}
