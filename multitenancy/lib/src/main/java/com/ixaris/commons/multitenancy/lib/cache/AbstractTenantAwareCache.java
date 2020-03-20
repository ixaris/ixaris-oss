package com.ixaris.commons.multitenancy.lib.cache;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.TenantLifecycleListener;
import com.ixaris.commons.multitenancy.lib.cache.AbstractTenantAwareCache.TenantKey;

public abstract class AbstractTenantAwareCache<K extends TenantKey, V> implements TenantLifecycleListener {
    
    public static abstract class TenantKey {
        
        public final String tenantId;
        
        public TenantKey() {
            tenantId = TENANT.get();
        }
        
        public TenantKey(final String tenantId) {
            this.tenantId = tenantId;
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, o, other -> tenantId.equals(other.tenantId));
        }
        
        @Override
        public int hashCode() {
            return tenantId.hashCode();
        }
        
    }
    
    public static final class StringTenantKey extends TenantKey {
        
        public final String key;
        
        public StringTenantKey(final String key) {
            this.key = key;
        }
        
        public StringTenantKey(final String tenantId, final String key) {
            super(tenantId);
            this.key = key;
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, o, other -> tenantId.equals(other.tenantId) && key.equals(other.key));
        }
        
        @Override
        public int hashCode() {
            return key.hashCode();
        }
        
    }
    
    public static final class LongTenantKey extends TenantKey {
        
        public final long key;
        
        public LongTenantKey(final long key) {
            this.key = key;
        }
        
        public LongTenantKey(final String tenantId, final long key) {
            super(tenantId);
            this.key = key;
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, o, other -> tenantId.equals(other.tenantId) && (key == other.key));
        }
        
        @Override
        public int hashCode() {
            return Long.hashCode(key);
        }
        
    }
    
    protected final MultiTenancy multiTenancy;
    protected final Cache<K, V> cache;
    
    public AbstractTenantAwareCache(final MultiTenancy multiTenancy, final int maxSize, final long expireAfterWriteMillis) {
        this.multiTenancy = multiTenancy;
        this.cache = CacheBuilder.newBuilder().maximumSize(maxSize).expireAfterWrite(Duration.of(expireAfterWriteMillis, ChronoUnit.MILLIS)).build();
    }
    
    @PostConstruct
    public void startup() {
        multiTenancy.addTenantLifecycleListener(this);
    }
    
    @PreDestroy
    public void shutdown() {
        multiTenancy.removeTenantLifecycleListener(this);
    }
    
    @Override
    public void onTenantActive(final String tenantId) {
        // no-op
    }
    
    @Override
    public void onTenantInactive(final String tenantId) {
        cache.asMap().entrySet().removeIf(next -> tenantId.equals(next.getKey().tenantId));
    }
    
    public long size() {
        return cache.size();
    }
    
    protected V get(final K key) {
        return cache.getIfPresent(key);
    }
    
    protected void put(final K key, final V value) {
        cache.put(key, value);
        postPut(key, value);
    }
    
    protected void invalidate(final K key) {
        final V value = cache.getIfPresent(key);
        if (value != null) {
            preRemove(key, value);
            cache.invalidate(key);
        }
    }
    
    protected void invalidateMatching(final Predicate<K> predicate) {
        cache
            .asMap()
            .entrySet()
            .removeIf(next -> {
                if (predicate.test(next.getKey())) {
                    preRemove(next.getKey(), next.getValue());
                    return true;
                } else {
                    return false;
                }
            });
    }
    
    protected void postPut(final K key, final V value) {}
    
    protected void preRemove(final K key, final V value) {}
    
}
