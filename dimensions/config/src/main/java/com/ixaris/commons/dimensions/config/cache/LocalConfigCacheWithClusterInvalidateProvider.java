package com.ixaris.commons.dimensions.config.cache;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.service.ClusterBroadcastHandler;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.InvalidateConfigCache;
import com.ixaris.commons.dimensions.config.ConfigDef;
import com.ixaris.commons.dimensions.config.SetDef;
import com.ixaris.commons.dimensions.config.SetDefRegistry;
import com.ixaris.commons.dimensions.config.ValueDefRegistry;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.cache.AbstractTenantAwareCache;
import com.ixaris.commons.multitenancy.lib.cache.AbstractTenantAwareCache.TenantKey;

@Component
public final class LocalConfigCacheWithClusterInvalidateProvider implements ConfigCacheProvider, ClusterBroadcastHandler<InvalidateConfigCache> {
    
    private static final int DEFAULT_SIZE = 5000;
    
    private static final class Key extends TenantKey {
        
        public final Context<? extends ConfigDef<?>> context;
        
        private Key(final Context<? extends ConfigDef<?>> context) {
            this.context = context;
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, o, other -> tenantId.equals(other.tenantId) && context.equals(other.context));
        }
        
        @Override
        public int hashCode() {
            return context.hashCode();
        }
        
    }
    
    private static final class LocalConfigCache extends AbstractTenantAwareCache<Key, Object> {
        
        private LocalConfigCache(final MultiTenancy multiTenancy, final int maxSize) {
            super(multiTenancy, maxSize, TimeUnit.HOURS.toMillis(1L));
        }
        
        private Object get(final Context<? extends ConfigDef<?>> key) {
            return get(new Key(key));
        }
        
        private void put(final Context<? extends ConfigDef<?>> key, final Object value) {
            put(new Key(key), value);
        }
        
        private void invalidate(final ConfigDef<?> def) {
            invalidateMatching(k -> k.context.getDef().equals(def));
        }
        
    }
    
    private final ClusterRegistry clusterRegistry;
    private final LocalConfigCache cacheable;
    
    @Autowired
    public LocalConfigCacheWithClusterInvalidateProvider(final MultiTenancy multiTenancy,
                                                         final ClusterRegistry clusterRegistry) {
        this(multiTenancy, clusterRegistry, DEFAULT_SIZE);
    }
    
    public LocalConfigCacheWithClusterInvalidateProvider(final MultiTenancy multiTenancy,
                                                         final ClusterRegistry clusterRegistry,
                                                         final int cacheableSize) {
        this.clusterRegistry = clusterRegistry;
        cacheable = new LocalConfigCache(multiTenancy, cacheableSize);
    }
    
    @PostConstruct
    public void startup() {
        clusterRegistry.register(this);
        cacheable.startup();
    }
    
    @PreDestroy
    public void shutdown() {
        cacheable.shutdown();
        clusterRegistry.deregister(this);
    }
    
    @Override
    public ConfigCacheWrapper of(final ConfigDef<?> def) {
        return new ConfigCacheWrapper(def);
    }
    
    @Override
    public String getKey() {
        return "config_invalidate";
    }
    
    @Override
    public Async<Boolean> handle(final InvalidateConfigCache message) {
        final ConfigDef<?> def = message.getSet()
            ? SetDefRegistry.getInstance().resolve(message.getKey()) : ValueDefRegistry.getInstance().resolve(message.getKey());
        invalidate(def);
        return result(true);
    }
    
    private void invalidate(final ConfigDef<?> def) {
        cacheable.invalidate(def);
    }
    
    private final class ConfigCacheWrapper implements ConfigCache {
        
        private final ConfigDef<?> def;
        
        private ConfigCacheWrapper(final ConfigDef<?> def) {
            this.def = def;
        }
        
        @Override
        public Object get(final Context<? extends ConfigDef<?>> key) {
            return key.isCacheable() ? cacheable.get(key) : null;
        }
        
        @Override
        public void put(final Context<? extends ConfigDef<?>> key, final Object value) {
            if (key.isCacheable()) {
                cacheable.put(key, value);
            }
        }
        
        @Override
        public void invalidate() {
            LocalConfigCacheWithClusterInvalidateProvider.this.invalidate(def);
            clusterRegistry.broadcast(
                LocalConfigCacheWithClusterInvalidateProvider.this,
                InvalidateConfigCache.newBuilder().setKey(def.getKey()).setSet(def instanceof SetDef).build());
        }
        
    }
    
}
