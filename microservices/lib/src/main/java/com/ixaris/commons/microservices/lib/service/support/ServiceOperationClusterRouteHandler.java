package com.ixaris.commons.microservices.lib.service.support;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ClusterRouteHandler;
import com.ixaris.commons.clustering.lib.service.ClusterRouteTimeoutException;
import com.ixaris.commons.microservices.lib.common.exception.ServerTimeoutException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;

public final class ServiceOperationClusterRouteHandler implements ClusterRouteHandler<RequestEnvelope, ResponseEnvelope> {
    
    private final ClusterRegistry clusterRegistry;
    private final ServiceSkeletonProxy<?> proxy;
    private final String key;
    
    public ServiceOperationClusterRouteHandler(final ClusterRegistry clusterRegistry, final ServiceSkeletonProxy<?> proxy, final String name) {
        this.clusterRegistry = clusterRegistry;
        this.proxy = proxy;
        key = "msop_" + ServiceSkeleton.extractServiceName(proxy.getSkeletonType()) + "_" + name;
    }
    
    public void startup() {
        clusterRegistry.register(this);
    }
    
    public void shutdown() {
        clusterRegistry.deregister(this);
    }
    
    @Override
    public String getKey() {
        return key;
    }
    
    @Override
    public Async<ResponseEnvelope> handle(final long id, final String key, final RequestEnvelope requestEnvelope) {
        return proxy.process(requestEnvelope);
    }
    
    public Async<ResponseEnvelope> route(final long shardKey, final RequestEnvelope request) {
        try {
            return awaitExceptions(clusterRegistry.route(this, shardKey, request));
        } catch (final ClusterRouteTimeoutException e) {
            throw new ServerTimeoutException(e);
        }
    }
    
}
