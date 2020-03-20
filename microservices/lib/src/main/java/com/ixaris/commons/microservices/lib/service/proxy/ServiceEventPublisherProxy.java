package com.ixaris.commons.microservices.lib.service.proxy;

import java.util.Arrays;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.proxy.ProxyInvoker;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;

final class ServiceEventPublisherProxy<T extends ServiceSkeleton> implements ProxyInvoker {
    
    private static final String GET_SKELETON_TYPE_METHOD_NAME = "getSkeletonType";
    private static final String GET_PATH_METHOD_NAME = "getPath";
    private static final String PUBLISH_METHOD_NAME = "publish";
    
    private final ServiceSkeletonProxy<T> proxy;
    private final ServicePathHolder path;
    
    ServiceEventPublisherProxy(final ServiceSkeletonProxy<T> proxy, final ServicePathHolder path) {
        this.proxy = proxy;
        this.path = path;
    }
    
    public ServicePathHolder getPath() {
        return path;
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
            } else if (GET_SKELETON_TYPE_METHOD_NAME.equals(methodName)) {
                return proxy.serviceSkeletonType;
            } else if (GET_PATH_METHOD_NAME.equals(methodName)) {
                return path;
            } else if (PUBLISH_METHOD_NAME.equals(methodName)) {
                final ServiceEventHeader<?> header = (ServiceEventHeader<?>) args[0];
                return proxy
                    .getPublisherForServiceKey(header.getTargetServiceKey())
                    .publish(header, (MessageLite) args[1], path);
            }
            
            throw new IllegalStateException("Unmatched method " + methodName);
            
        } catch (final RuntimeException e) {
            throw new IllegalStateException(String.format("Error while invoking method [%s] with args %s", methodName, Arrays.toString(args)), e);
        }
    }
    
}
