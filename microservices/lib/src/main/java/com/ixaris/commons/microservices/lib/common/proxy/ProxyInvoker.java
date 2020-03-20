package com.ixaris.commons.microservices.lib.common.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public interface ProxyInvoker extends InvocationHandler {
    
    @Override
    default Object invoke(final Object target, final Method method, final Object[] args) {
        return invoke(target, method.getName(), args);
    }
    
    Object invoke(Object target, String methodName, Object[] args);
    
}
