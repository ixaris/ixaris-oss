package com.ixaris.commons.microservices.lib.client.support;

import java.util.HashSet;
import java.util.Set;

import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.client.proxy.ServiceStubProxy;

public final class ServiceEventClusterRouteHandlerFactory {
    
    private final ClusterRegistry clusterRegistry;
    private final ServiceClientSupport serviceClientSupport;
    private final Set<ServiceEventClusterRouteHandler> handlers = new HashSet<>();
    
    public ServiceEventClusterRouteHandlerFactory(final ClusterRegistry clusterRegistry, final ServiceClientSupport serviceSupport) {
        this.clusterRegistry = clusterRegistry;
        this.serviceClientSupport = serviceSupport;
    }
    
    public ServiceEventClusterRouteHandler create(final Class<? extends ServiceStub> type, final String name) {
        final ServiceStubProxy<? extends ServiceStub> proxy = serviceClientSupport.getOrCreate(type);
        synchronized (handlers) {
            final ServiceEventClusterRouteHandler handler = new ServiceEventClusterRouteHandler(
                clusterRegistry, proxy, name);
            handler.startup();
            return handler;
        }
    }
    
    public void shutdown() {
        synchronized (handlers) {
            handlers.forEach(ServiceEventClusterRouteHandler::shutdown);
            handlers.clear();
        }
    }
    
}
