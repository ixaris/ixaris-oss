package com.ixaris.commons.multitenancy.lib.object;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.TestTenants;
import com.ixaris.commons.multitenancy.lib.exception.MissingMultiTenantObjectException;

public class AbstractEagerMultiTenantObjectTest {
    
    private MultiTenancy multiTenancy;
    private TestEagerMultiTenantObject multiTenantNamedObject;
    private TestEagerMultiTenantObject otherMultiTenantNamedObject;
    
    @Before
    public void start() {
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        multiTenantNamedObject = new TestEagerMultiTenantObject("TEST");
        otherMultiTenantNamedObject = new TestEagerMultiTenantObject("TEST2");
        multiTenancy.registerTenantLifecycleParticipant(multiTenantNamedObject);
    }
    
    @After
    public void stop() {
        multiTenancy.stop();
    }
    
    @Test
    public void addTenant_objectCreated() {
        multiTenantNamedObject.setTenantProp(TestTenants.DEFAULT, "testing");
        
        multiTenancy.addTenant(TestTenants.DEFAULT);
        
        assertTenantData(TestTenants.DEFAULT, "testing");
    }
    
    @Test
    public void getObject_tenantsDoNotShareObjects() {
        multiTenantNamedObject.setTenantProp(TestTenants.DEFAULT, TestTenants.DEFAULT);
        multiTenantNamedObject.setTenantProp("left", "left");
        
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.addTenant("left");
        
        // they should only be able to access their own version of the object
        assertTenantData(TestTenants.DEFAULT, TestTenants.DEFAULT);
        assertTenantData("left", "left");
    }
    
    @Test(expected = MissingMultiTenantObjectException.class)
    public void getObject_objectNotCreated_exceptionThrown() {
        multiTenantNamedObject.setTenantProp("left", "left");
        
        multiTenancy.addTenant("left");
        
        TENANT.exec("left", () -> otherMultiTenantNamedObject.get());
    }
    
    @Test
    public void removeTenant_objectDestroyed() throws Exception {
        multiTenantNamedObject.setTenantProp(TestTenants.DEFAULT, "destroying");
        
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.removeTenant(TestTenants.DEFAULT);
        
        assertDestroyedData(TestTenants.DEFAULT, "destroying");
    }
    
    private void assertTenantData(final String tenant, final String expectedData) {
        TENANT.exec(tenant, () -> {
            Assertions.assertThat(multiTenantNamedObject.get().getData()).isEqualTo(expectedData);
        });
    }
    
    private void assertDestroyedData(final String tenant, final String expectedData) {
        Assertions.assertThat(multiTenantNamedObject.getDestroyed(tenant).getData()).isEqualTo(expectedData);
    }
    
}
