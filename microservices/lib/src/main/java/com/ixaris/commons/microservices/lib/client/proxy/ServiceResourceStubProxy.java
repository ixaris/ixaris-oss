package com.ixaris.commons.microservices.lib.client.proxy;

import static com.ixaris.commons.async.lib.Async.from;
import static com.ixaris.commons.microservices.lib.common.ServiceConstants.WATCH_METHOD_NAME;

import java.util.Arrays;
import java.util.Collections;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.client.ServiceProviderStub;
import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.proxy.ProxyInvoker;

final class ServiceResourceStubProxy<T extends ServiceStub> implements ProxyInvoker {
    
    private final ServiceStubProxy<T> proxy;
    private final ServicePathHolder path;
    private final ServicePathHolder params;
    private final StubResourceInfo info;
    
    ServiceResourceStubProxy(final ServiceStubProxy<T> proxy, final ServicePathHolder path, final StubResourceInfo info) {
        this(proxy, path, ServicePathHolder.empty(), info);
    }
    
    private ServiceResourceStubProxy(final ServiceStubProxy<T> proxy,
                                     final ServicePathHolder path,
                                     final ServicePathHolder params,
                                     final StubResourceInfo info) {
        this.proxy = proxy;
        this.path = path;
        this.params = params;
        this.info = info;
    }
    
    @Override
    public Object invoke(final Object target, final String methodName, final Object[] args) {
        try {
            if ("equals".equals(methodName) && (args != null) && (args.length == 1)) {
                // Only consider equal when proxies are identical.
                return target == args[0];
            } else if ("hashCode".equals(methodName) && ((args == null) || (args.length == 0))) {
                return hashCode();
            } else if ("toString".equals(methodName) && ((args == null) || (args.length == 0))) {
                return toString();
            } else if (methodName.equals(ServiceProviderStub.KEYS_METHOD_NAME) && ((args == null) || (args.length == 0))) {
                return proxy.keysResolver.getKeys();
            } else if (methodName.equals(WATCH_METHOD_NAME)) {
                if (info.eventType == null) {
                    throw new IllegalStateException("Unmatched watch on resource " + path);
                }
                
                final ServiceEventListener<?, ?> listener = (ServiceEventListener<?, ?>) args[0];
                if (listener == null) {
                    throw new IllegalArgumentException("listener is null");
                }
                
                return proxy.subscribe(listener, info);
            } else if ((args == null) || args.length == 0) {
                // sub resource
                final StubResourceInfo resourceInfo = info.resourceMap.get(methodName);
                if (resourceInfo != null) {
                    // this path may have actual parameter values, so is rebuilt
                    return resourceInfo.proxyFactory.newInstance(new ServiceResourceStubProxy<>(proxy,
                        resourceInfo.path,
                        params,
                        resourceInfo));
                }
            } else if (ServiceOperationHeader.class.equals(args[0].getClass())) {
                // resource method
                final StubResourceMethodInfo<?, ?, ?> methodInfo = info.methodMap.get(methodName);
                if (methodInfo != null) {
                    return from(() -> proxy.invoke(path,
                        params,
                        methodInfo,
                        (ServiceOperationHeader<?>) args[0],
                        methodInfo.requestType != null ? (MessageLite) args[1] : null));
                }
            } else if (methodName.equals(info.paramName)) {
                // param resource
                final String arg = args[0].toString();
                return info.paramInfo.proxyFactory.newInstance(new ServiceResourceStubProxy<>(proxy,
                    info.paramInfo.path,
                    params.push(arg),
                    info.paramInfo));
            }
            
            throw new IllegalStateException("Unmatched method " + methodName);
            
        } catch (final RuntimeException e) {
            throw new IllegalStateException("Error while invoking method ["
                + methodName
                + "] with args "
                + Arrays.toString(args),
                e);
        }
    }
    
}
