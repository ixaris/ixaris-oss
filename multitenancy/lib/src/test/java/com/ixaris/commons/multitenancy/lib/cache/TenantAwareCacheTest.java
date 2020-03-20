package com.ixaris.commons.multitenancy.lib.cache;

import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.TestTenants.DEFAULT;
import static com.ixaris.commons.multitenancy.lib.TestTenants.LEFT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

public class TenantAwareCacheTest {
    
    private MultiTenancy multiTenancy;
    
    @Before
    public void start() {
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
    }
    
    @After
    public void stop() {
        multiTenancy.stop();
    }
    
    @Test
    public void testRemoveTenant() {
        multiTenancy.addTenant(DEFAULT);
        multiTenancy.addTenant(LEFT);
        
        final TestMultiTenancyAwareCache<String> cache = new TestMultiTenancyAwareCache<>(multiTenancy, 10);
        cache.startup();
        
        try {
            TENANT.exec(DEFAULT, () -> {
                cache.put("1", "1");
                cache.put("2", "2");
            });
            TENANT.exec(LEFT, () -> {
                cache.put("1", "1");
                cache.put("2", "2");
            });
            
            assertThat(cache.size()).isEqualTo(4);
            
            join(multiTenancy.removeTenant(LEFT));
            
            assertThat(cache.size()).isEqualTo(2);
        } finally {
            cache.shutdown();
        }
    }
    
    @Test
    public void testRemoveSubtree() {
        multiTenancy.addTenant(DEFAULT);
        multiTenancy.addTenant(LEFT);
        
        final TestMultiTenancyAwareCache<Tuple2<String, String>> cache = new TestMultiTenancyAwareCache<>(multiTenancy, 10);
        cache.startup();
        
        try {
            TENANT.exec(DEFAULT, () -> {
                cache.put("11", "1");
                cache.put("12", "2");
                cache.put("21", "1");
                cache.put("22", "2");
            });
            TENANT.exec(LEFT, () -> {
                cache.put("13", "3");
                cache.put("14", "4");
                cache.put("23", "3");
                cache.put("24", "4");
            });
            
            assertThat(cache.size()).isEqualTo(8);
            
            TENANT.exec(DEFAULT, () -> cache.invalidateMatching(k -> k.tenantId.equals(TENANT.get()) && k.key.startsWith("1")));
            
            assertThat(cache.size()).isEqualTo(6);
        } finally {
            cache.shutdown();
        }
    }
    
}
