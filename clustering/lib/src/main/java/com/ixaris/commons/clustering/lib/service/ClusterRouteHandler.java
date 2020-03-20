package com.ixaris.commons.clustering.lib.service;

import java.util.concurrent.TimeUnit;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.misc.lib.object.GenericsUtil;

public interface ClusterRouteHandler<REQ extends MessageLite, RES extends MessageLite> {
    
    @SuppressWarnings("unchecked")
    static <REQ extends MessageLite> Class<REQ> getRequestType(final ClusterRouteHandler<REQ, ?> handler) {
        return (Class<REQ>) GenericsUtil.resolveGenericTypeArguments(handler.getClass(), ClusterRouteHandler.class).get("REQ");
    }
    
    @SuppressWarnings("unchecked")
    static <RES extends MessageLite> Class<RES> getResponseType(final ClusterRouteHandler<?, RES> handler) {
        return (Class<RES>) GenericsUtil.resolveGenericTypeArguments(handler.getClass(), ClusterRouteHandler.class).get("RES");
    }
    
    String getKey();
    
    Async<RES> handle(long id, String key, REQ request);
    
    default long getTimeout() {
        return Defaults.DEFAULT_TIMEOUT;
    }
    
    default TimeUnit getTimeoutUnit() {
        return TimeUnit.MILLISECONDS;
    }
    
}
