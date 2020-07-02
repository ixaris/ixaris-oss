package com.ixaris.commons.hikari.persistence;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;

import java.sql.SQLException;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.datasource.AbstractMultiTenantDataSource;
import com.ixaris.commons.multitenancy.lib.exception.MissingMultiTenantObjectException;
import com.ixaris.commons.multitenancy.spring.MultiTenancyTenants;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.persistence.lib.datasource.MultiTenancyDataSourceConfig;
import com.ixaris.commons.persistence.lib.datasource.MultiTenancyDataSourceDefaultsConfig;

/**
 * @author benjie.gatt
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DataSourceTestConfiguration.class })
@TestPropertySource(locations = "classpath:test-property-config.yml")
public class HikariMultiTenantDataSourceIT {
    
    public static final String UNIT_NAME = "unit_hikari";
    
    @Autowired
    private MultiTenancyTenants tenants;
    
    @Autowired
    private MultiTenancyDataSourceConfig config;
    
    @Autowired
    private AbstractMultiTenantDataSource dataSource;
    
    @Autowired
    private MultiTenancy multiTenancy;
    
    @BeforeClass
    public static void beforeClass() {
        System.setProperty("spring.application.name", UNIT_NAME);
    }
    
    @Before
    public void setUp() throws SQLException {
        multiTenancy.addTenant(MultiTenancy.SYSTEM_TENANT);
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.addTenant(TestTenants.LEFT);
        multiTenancy.addTenant(TestTenants.RIGHT);
        multiTenancy.addTenant("poolMissing");
        multiTenancy.addTenant("urlMissing");
        multiTenancy.addTenant("userMissing");
        multiTenancy.addTenant("passwordMissing");
        multiTenancy.addTenant("usesDefault");
        
        DATA_UNIT.exec("unit_hikari", () -> TENANT.exec(TestTenants.DEFAULT, QueryCallableFactory.getInsertDataCallable(dataSource, "defaultmustache")));
        DATA_UNIT.exec("unit_hikari", () -> TENANT.exec(TestTenants.LEFT, QueryCallableFactory.getInsertDataCallable(dataSource, "leftmustache")));
        DATA_UNIT.exec("unit_hikari", () -> TENANT.exec("poolMissing", QueryCallableFactory.getInsertDataCallable(dataSource, "poolMissingMustache")));
        DATA_UNIT.exec("unit_hikari", () -> TENANT.exec("usesDefault", QueryCallableFactory.getInsertDataCallable(dataSource, "usesDefaultMustache")));
    }
    
    @Test
    public void tenantNamesLoadedFromProperties() {
        Assertions
            .assertThat(tenants.getTenants())
            .containsExactlyInAnyOrder(TestTenants.DEFAULT, TestTenants.LEFT, TestTenants.RIGHT, "poolMissing", "urlMissing", "userMissing", "passwordMissing");
    }
    
    @Test
    public void singleTenantFailed_otherTenantsStillWork() throws SQLException {
        final String defaultTenant = DATA_UNIT.exec("unit_hikari", () -> TENANT.exec(TestTenants.DEFAULT, QueryCallableFactory.getStacheNameQueryCallable(dataSource)));
        Assertions.assertThat(defaultTenant).isEqualTo("defaultmustache");
        final String leftTenant = DATA_UNIT.exec("unit_hikari", () -> TENANT.exec(TestTenants.LEFT, QueryCallableFactory.getStacheNameQueryCallable(dataSource)));
        Assertions.assertThat(leftTenant).isEqualTo("leftmustache");
        
        try {
            DATA_UNIT.exec("unit_hikari", () -> TENANT.exec(TestTenants.RIGHT, QueryCallableFactory.getStacheNameQueryCallable(dataSource)));
        } catch (MissingMultiTenantObjectException e) {
            // wasn't exactly sure what to assert...the important thing is that THIS is the exception that is thrown -
            // all others will be caught by the test and cause a failure
            // expected on the test annotation would have been misleading
            Assertions.assertThat(e).isNotNull();
        }
    }
    
    @Test
    public void tenantWithMissingConnectionPoolProperties_tenantHasDataSourceWithDefaultPoolProperties() throws SQLException {
        final String poolMissing = DATA_UNIT.exec("unit_hikari", () -> TENANT.exec("poolMissing", QueryCallableFactory.getStacheNameQueryCallable(dataSource)));
        Assertions.assertThat(poolMissing).isEqualTo("poolMissingMustache");
    }
    
    @Test
    public void tenantWithNoSpecificConfig_tenantUsesDefaultConfig() throws SQLException {
        final String usesDefault = DATA_UNIT.exec("unit_hikari", () -> TENANT.exec("usesDefault", QueryCallableFactory.getStacheNameQueryCallable(dataSource)));
        Assertions.assertThat(usesDefault).isEqualTo("usesDefaultMustache");
    }
    
    @Test(expected = MissingMultiTenantObjectException.class)
    public void tenantWithNoSpecificConfig_noDefaultConfigSpecified_tenantHasNoDataSource() throws SQLException {
        // simulate configuration with no defaults
        config.setDefaults(new MultiTenancyDataSourceDefaultsConfig());
        // add a tenant with no specific configuration
        multiTenancy.addTenant("noDefaults");
        
        // should fail, as there is no db setup due to errors above
        DATA_UNIT.exec("unit_hikari", () -> TENANT.exec("noDefaults", QueryCallableFactory.getStacheNameQueryCallable(dataSource)));
    }
    
    @Test(expected = MissingMultiTenantObjectException.class)
    public void singleTenantWithNoConfiguration_tenantHasNoDataSource() throws SQLException {
        DATA_UNIT.exec("unit_hikari", () -> TENANT.exec("urlMissing", QueryCallableFactory.getStacheNameQueryCallable(dataSource)));
    }
    
    @Test(expected = MissingMultiTenantObjectException.class)
    public void singleTenantWithMissingUrl_tenantHasNoDataSource() throws SQLException {
        DATA_UNIT.exec("unit_hikari", () -> TENANT.exec("urlMissing", QueryCallableFactory.getStacheNameQueryCallable(dataSource)));
    }
    
    @Test(expected = MissingMultiTenantObjectException.class)
    public void singleTenantWithMissingUser_tenantHasNoDataSource() throws SQLException {
        DATA_UNIT.exec("unit_hikari", () -> TENANT.exec("userMissing", QueryCallableFactory.getStacheNameQueryCallable(dataSource)));
    }
    
    @Test(expected = MissingMultiTenantObjectException.class)
    public void singleTenantWithMissingPassword_tenantHasNoDataSource() throws SQLException {
        DATA_UNIT.exec("unit_hikari", () -> TENANT.exec("passwordMissing", QueryCallableFactory.getStacheNameQueryCallable(dataSource)));
    }
    
    // TODO: more migration tests with multiple tenants in different configs (tenants being removed etc)
    
}
