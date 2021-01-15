package com.ixaris.commons.dimensions.limits.cache;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.dimensions.lib.base.DimensionalDef;
import com.ixaris.commons.dimensions.limits.CounterLimitDefRegistry;
import com.ixaris.commons.dimensions.limits.LimitDef;
import com.ixaris.commons.dimensions.limits.ValueLimitDefRegistry;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;
import com.ixaris.commons.misc.lib.object.Reference;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.object.LazyMultiTenantObject;

@Component
public final class LocalLimitCacheWithClusterInvalidateProvider implements LimitsCacheProvider {
    
    private final MultiTenancy multiTenancy;
    private final Map<DimensionalDef, LimitsCache<?, ?>> VALUES = new HashMap<>();
    
    @Autowired
    public LocalLimitCacheWithClusterInvalidateProvider(final MultiTenancy multiTenancy) {
        this.multiTenancy = multiTenancy;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> CacheAdapter<I, L> of(final L def) {
        synchronized (VALUES) {
            return (CacheAdapter<I, L>) VALUES.computeIfAbsent(def, k -> new CacheAdapter(def));
        }
    }
    
    private final class CacheAdapter<I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> implements LimitsCache<I, L> {
        
        private final L def;
        private final LazyMultiTenantObject<Reference<LimitCacheEntry<I, L>>> cache = new LazyMultiTenantObject<>(
            multiTenancy, Reference::new);
        
        public CacheAdapter(L def) {
            this.def = def;
        }
        
        @Override
        public LimitCacheEntry<I, L> get() {
            return cache.get().get();
        }
        
        @Override
        public void set(final LimitCacheEntry<I, L> value) {
            cache.get().set(value);
        }
        
        @Override
        public void clear() {
            clearWithoutBroadcast();
        }
        
        private void clearWithoutBroadcast() {
            cache.get().set(null);
        }
        
    }
    
}
