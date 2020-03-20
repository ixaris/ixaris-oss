package com.ixaris.commons.multitenancy.lib.cache;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ClusterShardsChangeListener;
import com.ixaris.commons.collections.lib.BitSet;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.cache.AbstractTenantAwareCache.TenantKey;

public abstract class AbstractTenantAndShardAwareCache<K extends TenantKey, V> extends AbstractTenantAwareCache<K, V> implements ClusterShardsChangeListener {
    
    protected final ClusterRegistry clusterRegistry;
    
    public AbstractTenantAndShardAwareCache(final MultiTenancy multiTenancy,
                                            final ClusterRegistry clusterRegistry,
                                            final int maxSize,
                                            final long expireAfterWriteMillis) {
        super(multiTenancy, maxSize, expireAfterWriteMillis);
        this.clusterRegistry = clusterRegistry;
    }
    
    protected abstract int getShardFromKey(K key);
    
    @PostConstruct
    @Override
    public void startup() {
        super.startup();
        clusterRegistry.addShardsListener(this);
    }
    
    @PreDestroy
    @Override
    public void shutdown() {
        clusterRegistry.removeShardsListener(this);
        super.shutdown();
    }
    
    @Override
    public void onShardsChanged(final BitSet oldShards, final BitSet newShards) {
        boolean shardRemoved = false;
        for (int i = 0; i < clusterRegistry.getMaxShards(); i++) {
            if (oldShards.contains(i) && !newShards.contains(i)) {
                shardRemoved = true;
                break;
            }
        }
        if (shardRemoved) {
            invalidateMatching(k -> !newShards.contains(getShardFromKey(k)));
        }
    }
    
}
