package com.ixaris.commons.multitenancy.lib.datasource;

import java.lang.reflect.Field;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.TestTenants;
import com.ixaris.commons.multitenancy.lib.object.AbstractEagerMultiTenantSharedObject;

public class AbstractMultiTenantDataSourceTest {
    
    private MultiTenancy multiTenancy;
    private TestMultiTenancyDataSource dataSource;
    
    @Before
    public void start() {
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        dataSource = new TestMultiTenancyDataSource("mysql");
        multiTenancy.registerTenantLifecycleParticipant(dataSource);
    }
    
    @After
    public void stop() {
        multiTenancy.stop();
    }
    
    @Test
    public void activatingEventReceived_dataSourcePreparedForTenant() {
        multiTenancy.addTenant(TestTenants.DEFAULT);
        
        Assertions.assertThat(getDataSources(dataSource).size()).isEqualTo(1);
        Assertions.assertThat(getUsageCount(getDataSources(dataSource).values().iterator().next())).isEqualTo(1);
    }
    
    @Test
    public void inactiveEventReceived_tenantDataSourceRemoved() {
        multiTenancy.addTenant(TestTenants.DEFAULT);
        Assertions.assertThat(getDataSources(dataSource).size()).isEqualTo(1);
        Assertions.assertThat(getUsageCount(getDataSources(dataSource).values().iterator().next())).isEqualTo(1);
        
        multiTenancy.removeTenant(TestTenants.DEFAULT);
        Assertions.assertThat(getDataSources(dataSource).size()).isEqualTo(0);
    }
    
    @Test
    public void garbageCollection_datasourceWithActiveTenantsNotCleanedUp() {
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.addTenant("left");
        Assertions.assertThat(getDataSources(dataSource).size()).isEqualTo(1);
        Assertions.assertThat(getUsageCount(getDataSources(dataSource).values().iterator().next())).isEqualTo(2);
        
        multiTenancy.removeTenant(TestTenants.DEFAULT);
        Assertions.assertThat(getDataSources(dataSource).size()).isEqualTo(1);
        Assertions.assertThat(getUsageCount(getDataSources(dataSource).values().iterator().next())).isEqualTo(1);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getDataSources(final AbstractMultiTenantDataSource dataSource) {
        try {
            final Field instances = AbstractEagerMultiTenantSharedObject.class.getDeclaredField("instances");
            instances.setAccessible(true);
            return (Map<String, Object>) instances.get(dataSource);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private int getUsageCount(final Object ds) {
        try {
            final Field usageCount = ds.getClass().getDeclaredField("usageCount");
            usageCount.setAccessible(true);
            return (int) usageCount.get(ds);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
    
}
