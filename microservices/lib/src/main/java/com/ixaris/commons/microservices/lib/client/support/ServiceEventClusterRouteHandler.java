package com.ixaris.commons.microservices.lib.client.support;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ClusterRouteHandler;
import com.ixaris.commons.clustering.lib.service.ClusterRouteTimeoutException;
import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.client.proxy.ServiceStubProxy;
import com.ixaris.commons.microservices.lib.common.exception.ServerTimeoutException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;

public final class ServiceEventClusterRouteHandler implements ClusterRouteHandler<EventEnvelope, EventAckEnvelope> {
    
    private final ClusterRegistry clusterRegistry;
    private final ServiceStubProxy<?> proxy;
    private final String name;
    private final String key;
    
    public ServiceEventClusterRouteHandler(final ClusterRegistry clusterRegistry, final ServiceStubProxy<?> proxy, final String name) {
        this.clusterRegistry = clusterRegistry;
        this.proxy = proxy;
        this.name = name;
        key = "msev_" + ServiceStub.extractServiceName(proxy.getStubType()) + "_" + name;
    }
    
    @PostConstruct
    public void startup() {
        clusterRegistry.register(this);
    }
    
    @PreDestroy
    public void shutdown() {
        clusterRegistry.deregister(this);
    }
    
    @Override
    public String getKey() {
        return key;
    }
    
    @Override
    public Async<EventAckEnvelope> handle(final long id, final String key, final EventEnvelope eventEnvelope) {
        return proxy.process(eventEnvelope, name);
    }
    
    public Async<EventAckEnvelope> route(final long shardKey, final EventEnvelope request) {
        try {
            return awaitExceptions(clusterRegistry.route(this, shardKey, request));
        } catch (final ClusterRouteTimeoutException e) {
            throw new ServerTimeoutException(e);
        }
    }
    
}
