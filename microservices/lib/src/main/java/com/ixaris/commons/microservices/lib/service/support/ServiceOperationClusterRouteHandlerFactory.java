package com.ixaris.commons.microservices.lib.service.support;

import java.util.HashSet;
import java.util.Set;

import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;

public final class ServiceOperationClusterRouteHandlerFactory {
    
    private final ClusterRegistry clusterRegistry;
    private final ServiceSupport serviceSupport;
    private final Set<ServiceOperationClusterRouteHandler> handlers = new HashSet<>();
    
    public ServiceOperationClusterRouteHandlerFactory(final ClusterRegistry clusterRegistry, final ServiceSupport serviceSupport) {
        this.clusterRegistry = clusterRegistry;
        this.serviceSupport = serviceSupport;
    }
    
    public ServiceOperationClusterRouteHandler create(final Class<? extends ServiceSkeleton> type, final String name) {
        final ServiceSkeletonProxy<?> proxy = serviceSupport.getOrCreate(type);
        synchronized (handlers) {
            final ServiceOperationClusterRouteHandler handler = new ServiceOperationClusterRouteHandler(
                clusterRegistry, proxy, name);
            handler.startup();
            return handler;
        }
    }
    
    public void shutdown() {
        synchronized (handlers) {
            handlers.forEach(ServiceOperationClusterRouteHandler::shutdown);
            handlers.clear();
        }
    }
    
}
