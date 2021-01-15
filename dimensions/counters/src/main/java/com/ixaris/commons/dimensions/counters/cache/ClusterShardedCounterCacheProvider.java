package com.ixaris.commons.dimensions.counters.cache;

import static com.ixaris.commons.clustering.lib.common.ClusterShardResolver.getShardKeyFromString;
import static com.ixaris.commons.dimensions.counters.CountersHelper.determineShardKey;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jooq.UpdatableRecord;

import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.dimensions.counters.CounterDef;
import com.ixaris.commons.dimensions.counters.WindowWidth;
import com.ixaris.commons.dimensions.counters.cache.ClusterShardedCounterCacheProvider.Key;
import com.ixaris.commons.dimensions.counters.data.CounterEntity;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.cache.AbstractTenantAndShardAwareCache;
import com.ixaris.commons.multitenancy.lib.cache.AbstractTenantAwareCache;

public final class ClusterShardedCounterCacheProvider extends AbstractTenantAndShardAwareCache<Key<?, ?>, CounterEntity<?, ?>> implements CounterCacheProvider {
    
    public static final int DEFAULT_SIZE = 1000;
    
    public static final class Key<R extends UpdatableRecord<R>, C extends CounterDef<R, C>> extends AbstractTenantAwareCache.TenantKey {
        
        public final C def;
        public final Context<C> context;
        public final WindowWidth narrowWindowWidth;
        public final int wideWindowMultiple;
        
        public Key(final C def, final Context<C> context, final WindowWidth narrowWindowWidth, final int wideWindowMultiple) {
            this.def = def;
            this.context = context;
            this.narrowWindowWidth = narrowWindowWidth;
            this.wideWindowMultiple = wideWindowMultiple;
        }
        
        @SuppressWarnings("squid:S1067")
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, o, other -> tenantId.equals(other.tenantId)
                && def.equals(other.def)
                && context.equals(other.context)
                && narrowWindowWidth.equals(other.narrowWindowWidth)
                && (wideWindowMultiple == other.wideWindowMultiple));
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(def, context, narrowWindowWidth, wideWindowMultiple);
        }
        
    }
    
    private final ClusterRegistry clusterRegistry;
    
    public ClusterShardedCounterCacheProvider(final MultiTenancy multiTenancy, final ClusterRegistry clusterRegistry) {
        this(multiTenancy, clusterRegistry, DEFAULT_SIZE);
    }
    
    public ClusterShardedCounterCacheProvider(final MultiTenancy multiTenancy, final ClusterRegistry clusterRegistry, final int size) {
        super(multiTenancy, clusterRegistry, size, TimeUnit.HOURS.toMillis(1L));
        this.clusterRegistry = clusterRegistry;
    }
    
    @Override
    protected int getShardFromKey(final Key<?, ?> key) {
        return internalGetShardFromKey(key);
    }
    
    private <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> int internalGetShardFromKey(final Key<R, C> key) {
        final Dimension<?> pDimension = CounterDef.extractFirstPartitionDimension(key.def, key.context);
        return clusterRegistry.getShard((pDimension != null) ? determineShardKey(pDimension.getPersistedValue()) : getShardKeyFromString(key.tenantId));
    }
    
    @PostConstruct
    @Override
    public void startup() {
        clusterRegistry.addShardsListener(this);
        super.startup();
    }
    
    @PreDestroy
    @Override
    public void shutdown() {
        super.shutdown();
        clusterRegistry.removeShardsListener(this);
    }
    
    @Override
    public <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> CounterCache<R, C> of(final C def) {
        return new CounterCacheWrapper<>(def);
    }
    
    private final class CounterCacheWrapper<R extends UpdatableRecord<R>, C extends CounterDef<R, C>> implements CounterCache<R, C> {
        
        private final C def;
        
        private CounterCacheWrapper(final C def) {
            this.def = def;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public CounterEntity<R, C> get(final Context<C> context, final WindowWidth narrowWindowWidth, final int wideWindowMultiple) {
            return (CounterEntity<R, C>) ClusterShardedCounterCacheProvider.this.get(
                new Key<>(def, context, narrowWindowWidth, wideWindowMultiple));
        }
        
        @Override
        public void put(final CounterEntity<R, C> value) {
            ClusterShardedCounterCacheProvider.this.put(
                new Key<>(def, value.getContext(), value.getNarrowWindowWidth(), value.getWideWindowMultiple()), value);
        }
        
        @Override
        public void invalidate(final Context<C> context, final WindowWidth narrowWindowWidth, final int wideWindowMultiple) {
            ClusterShardedCounterCacheProvider.this.invalidate(
                new Key<>(def, context, narrowWindowWidth, wideWindowMultiple));
        }
        
        @Override
        public void clear() {
            final String tenantId = TENANT.get();
            invalidateMatching(k -> k.def.equals(def) && k.tenantId.equals(tenantId));
        }
        
    }
    
}
