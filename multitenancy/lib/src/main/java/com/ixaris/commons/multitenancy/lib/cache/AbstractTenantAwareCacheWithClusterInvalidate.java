package com.ixaris.commons.multitenancy.lib.cache;

import static com.ixaris.commons.async.lib.Async.result;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.service.ClusterBroadcastHandler;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.cache.AbstractTenantAwareCache.TenantKey;

public abstract class AbstractTenantAwareCacheWithClusterInvalidate<C extends MessageLite, K extends TenantKey, V> extends AbstractTenantAwareCache<K, V> implements ClusterBroadcastHandler<C> {
    
    private final ClusterRegistry clusterRegistry;
    
    public AbstractTenantAwareCacheWithClusterInvalidate(final MultiTenancy multiTenancy,
                                                         final ClusterRegistry clusterRegistry,
                                                         final int maxSize,
                                                         final long expireAfterWriteMillis) {
        super(multiTenancy, maxSize, expireAfterWriteMillis);
        this.clusterRegistry = clusterRegistry;
    }
    
    @Override
    public void startup() {
        super.startup();
        clusterRegistry.register(this);
    }
    
    @Override
    public void shutdown() {
        clusterRegistry.deregister(this);
        super.shutdown();
    }
    
    @Override
    public Async<Boolean> handle(final C message) {
        super.invalidate(fromInvalidateBroadcastKey(message));
        return result(true);
    }
    
    @Override
    protected void put(final K key, final V value) {
        super.put(key, value);
        clusterRegistry.broadcast(this, toInvalidateBroadcastKey(key));
    }
    
    @Override
    protected void invalidate(final K key) {
        super.invalidate(key);
        clusterRegistry.broadcast(this, toInvalidateBroadcastKey(key));
    }
    
    protected abstract C toInvalidateBroadcastKey(K key);
    
    protected abstract K fromInvalidateBroadcastKey(C broadcastKey);
    
}
