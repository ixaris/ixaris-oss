package com.ixaris.commons.multitenancy.lib.cache;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.cache.AbstractTenantAwareCache.StringTenantKey;

public final class TestMultiTenancyAwareCache<K> extends AbstractTenantAwareCache<StringTenantKey, String> {
    
    public TestMultiTenancyAwareCache(final MultiTenancy multiTenancy, final int maxSize) {
        super(multiTenancy, maxSize, TimeUnit.HOURS.toMillis(1L));
    }
    
    public String get(final String key) {
        return get(new StringTenantKey(key));
    }
    
    public void put(final String key, final String value) {
        put(new StringTenantKey(key), value);
    }
    
}
