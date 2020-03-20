package com.ixaris.commons.clustering.lib.service;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.misc.lib.object.GenericsUtil;

public interface ClusterBroadcastHandler<T extends MessageLite> {
    
    @SuppressWarnings("unchecked")
    static <T extends MessageLite> Class<T> getType(final ClusterBroadcastHandler<T> handler) {
        return (Class<T>) GenericsUtil.resolveGenericTypeArguments(handler.getClass(), ClusterBroadcastHandler.class).get("T");
    }
    
    String getKey();
    
    Async<Boolean> handle(T message);
    
}
