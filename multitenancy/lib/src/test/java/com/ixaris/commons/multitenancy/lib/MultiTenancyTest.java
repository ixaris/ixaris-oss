package com.ixaris.commons.multitenancy.lib;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MultiTenancyTest {
    
    private MultiTenancy multiTenancy;
    
    @BeforeEach
    public void start() {
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.addTenant(TestTenants.LEFT);
        multiTenancy.addTenant(TestTenants.RIGHT);
    }
    
    @AfterEach
    public void stop() {
        multiTenancy.stop();
    }
    
    @Test
    public void testGetCurrentTenant_nullTenant_nullTenantNotAllowed() {
        assertThatThrownBy(() -> MultiTenancy.getCurrentTenant(false)).isInstanceOf(IllegalStateException.class);
    }
    
    @Test
    public void testGetCurrentTenant_successActiveTenant() {
        TENANT.exec(TestTenants.DEFAULT, () -> assertThat(MultiTenancy.getCurrentTenant(false)).isEqualTo(TestTenants.DEFAULT));
    }
    
    @Test
    public void testGetCurrentTenant_successNullTenant_nullTenantAllowed() {
        assertThat(MultiTenancy.getCurrentTenant(true)).isNull();
    }
    
    @Test
    public void testDoAsTenant_nullTenantId() {
        assertThatThrownBy(() -> TENANT.exec((String) null, () -> {})).isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void testDoAsTenant_differentTenantActive() {
        assertThatThrownBy(() -> TENANT.exec(TestTenants.DEFAULT, () -> TENANT.exec(TestTenants.LEFT, () -> {}))).isInstanceOf(IllegalStateException.class);
    }
    
    /**
     * doAsTenant should support reentrancy. A tenant can be re-activate even if it is currently active
     */
    @Test
    public void testDoAsTenant_successReEntrant() {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            TENANT.exec(TestTenants.DEFAULT, () -> {
                assertThat(MultiTenancy.getCurrentTenant(false)).isEqualTo(TestTenants.DEFAULT);
            });
        });
    }
    
    @Test
    public void testDoAsTenant_successWithReturn() {
        final String result = TENANT.exec(TestTenants.DEFAULT, () -> MultiTenancy.getCurrentTenant(false));
        assertThat(result).isEqualTo(TestTenants.DEFAULT);
    }
    
    /**
     * Should never be allowed to use doAsAllTenant when there already is a tenant active on current thread.
     */
    @Test
    public void testDoAsAllTenants_someTenantAlreadyActive_failure() {
        assertThatThrownBy(() -> TENANT.exec(TestTenants.DEFAULT, multiTenancy::getActiveTenants)).isInstanceOf(IllegalStateException.class);
    }
    
    @Test
    public void testDoAsAllTenants_success() {
        final List<String> tenants = new ArrayList<>();
        tenants.add(TestTenants.DEFAULT);
        tenants.add(TestTenants.LEFT);
        tenants.add(TestTenants.RIGHT);
        
        multiTenancy
            .getActiveTenants()
            .forEach(t -> TENANT.exec(t, () -> {
                // We need to do action ONCE per tenant, therefore make sure that tenant id still existed in list.
                assertTrue(tenants.remove(MultiTenancy.getCurrentTenant()));
            }));
        
        // Check that all tenant ids where consumed
        assertThat(tenants).hasSize(0);
    }
    
    @Test
    public void testDoAsAllTenants_successExceptionDuringTask() {
        final List<String> tenants = new ArrayList<>();
        tenants.add(TestTenants.DEFAULT);
        tenants.add(TestTenants.LEFT);
        tenants.add(TestTenants.RIGHT);
        
        final Map<String, Exception> results = new HashMap<>();
        multiTenancy
            .getActiveTenants()
            .forEach(t -> TENANT.exec(t, () -> {
                // We need to do action ONCE per tenant, therefore make sure that tenant id still existed in list.
                assertTrue(tenants.remove(MultiTenancy.getCurrentTenant()));
                results.put(t, new Exception(MultiTenancy.getCurrentTenant()));
            }));
        
        // Check that all tenant ids where consumed
        assertThat(tenants).hasSize(0);
        
        // Check that exceptions were recorded for the correct tenants
        assertThat(results.get(TestTenants.DEFAULT).getMessage()).isEqualTo(TestTenants.DEFAULT);
        assertThat(results.get(TestTenants.LEFT).getMessage()).isEqualTo(TestTenants.LEFT);
        assertThat(results.get(TestTenants.RIGHT).getMessage()).isEqualTo(TestTenants.RIGHT);
    }
    
    /**
     * Should not be allowed to get information about other tenants when there is already a tenant active
     */
    @Test
    public void testGetAllTenants_tenantActive() {
        assertThrows(IllegalStateException.class, () -> TENANT.exec(TestTenants.DEFAULT, multiTenancy::getActiveTenants));
    }
    
    @Test
    public void testGetAllTenants_success() {
        final Set<String> tenants = multiTenancy.getActiveTenants();
        
        // Check result
        assertThat(tenants).hasSize(3);
        assertThat(tenants).contains(TestTenants.DEFAULT);
        assertThat(tenants).contains(TestTenants.LEFT);
        assertThat(tenants).contains(TestTenants.RIGHT);
    }
    
    @Test
    public void testHasTenant_successWithTenant() {
        assertThat(multiTenancy.isTenantActive(TestTenants.DEFAULT)).isTrue();
    }
    
    @Test
    public void testHasTenant_successNoTenant() {
        assertThat(multiTenancy.isTenantActive("some_string")).isFalse();
    }
    
    @Test
    public void testHasTenant_successSameTenantActive() {
        assertThat(TENANT.exec(TestTenants.DEFAULT, () -> multiTenancy.isTenantActive(TestTenants.DEFAULT))).isTrue();
    }
    
    @Test
    public void testHasTenant_differentTenantActive() {
        assertThrows(IllegalStateException.class, () -> TENANT.exec(TestTenants.DEFAULT, () -> multiTenancy.isTenantActive(TestTenants.LEFT)));
    }
    
    /**
     * If you already have a tenant active you should not be asking for the default tenant
     */
    @Test
    public void testGetDefaultTenant_failure_someTenantAlreadyActive() {
        assertThrows(IllegalStateException.class, () -> TENANT.exec(TestTenants.DEFAULT, multiTenancy::getDefaultTenant));
    }
    
    /**
     * Test that no default tenant is found if there are multiple tenants
     */
    @Test
    public void testGetDefaultTenant_failure_multipleTenantsLoaded() {
        assertThatThrownBy(multiTenancy::getDefaultTenant).isInstanceOf(IllegalStateException.class);
    }
    
    /**
     * Only a single tenant loaded, and no active tenants. Return the tenant successfully
     */
    @Test
    public void testGetDefaultTenant_success() {
        multiTenancy.stop();
        
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        multiTenancy.addTenant(TestTenants.DEFAULT);
        
        assertThat(multiTenancy.getDefaultTenant()).isEqualTo(TestTenants.DEFAULT);
    }
    
    @Test
    public void testEvents() {
        multiTenancy.stop();
        
        final TestListener listener = new TestListener();
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        multiTenancy.addTenant(TestTenants.DEFAULT);
        
        assertThat(listener.getActivated().size()).isEqualTo(0);
        assertThat(listener.getDeactivated().size()).isEqualTo(0);
        
        multiTenancy.addTenantLifecycleListener(listener);
        
        assertThat(listener.getActivated().size()).isEqualTo(1);
        assertThat(listener.getActivated()).isEqualTo(Collections.singletonList(TestTenants.DEFAULT));
        assertThat(listener.getDeactivated().size()).isEqualTo(0);
        
        multiTenancy.addTenant(TestTenants.LEFT);
        
        assertThat(listener.getActivated().size()).isEqualTo(2);
        assertThat(listener.getActivated()).isEqualTo(Arrays.asList(TestTenants.DEFAULT, TestTenants.LEFT));
        assertThat(listener.getDeactivated().size()).isEqualTo(0);
        
        multiTenancy.addTenant(TestTenants.RIGHT);
        
        assertThat(listener.getActivated().size()).isEqualTo(3);
        assertThat(listener.getActivated()).isEqualTo(Arrays.asList(TestTenants.DEFAULT, TestTenants.LEFT, TestTenants.RIGHT));
        assertThat(listener.getDeactivated().size()).isEqualTo(0);
        
        multiTenancy.removeTenant(TestTenants.LEFT);
        
        assertThat(listener.getActivated().size()).isEqualTo(3);
        assertThat(listener.getActivated()).isEqualTo(Arrays.asList(TestTenants.DEFAULT, TestTenants.LEFT, TestTenants.RIGHT));
        assertThat(listener.getDeactivated().size()).isEqualTo(1);
        assertThat(listener.getDeactivated()).isEqualTo(Collections.singletonList(TestTenants.LEFT));
        
        multiTenancy.addTenant(TestTenants.LEFT);
        
        assertThat(listener.getActivated().size()).isEqualTo(4);
        assertThat(listener.getActivated()).isEqualTo(Arrays.asList(TestTenants.DEFAULT, TestTenants.LEFT, TestTenants.RIGHT, TestTenants.LEFT));
        assertThat(listener.getDeactivated().size()).isEqualTo(1);
        assertThat(listener.getDeactivated()).isEqualTo(Collections.singletonList(TestTenants.LEFT));
    }
    
}
