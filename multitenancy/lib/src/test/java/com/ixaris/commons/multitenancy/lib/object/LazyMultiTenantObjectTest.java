package com.ixaris.commons.multitenancy.lib.object;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.TestTenants;
import com.ixaris.commons.multitenancy.lib.object.TestLazyMultiTenantObject.TestContainer;

/**
 * Test TenantObject.
 *
 * @author <a href="mailto:matthias.portelli@ixaris.com">matthias.portelli</a>
 */
public class LazyMultiTenantObjectTest {
    
    private MultiTenancy multiTenancy;
    private LazyMultiTenantObject<TestContainer> obj;
    
    @Before
    public void start() {
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.addTenant(TestTenants.LEFT);
        multiTenancy.addTenant(TestTenants.RIGHT);
        
        obj = new TestLazyMultiTenantObject(multiTenancy, "TEST");
    }
    
    @After
    public void stop() {
        multiTenancy.stop();
    }
    
    @Test
    public void getObject_objectNotCreatedPreviously_newObjectInstantiatedAndReused() {
        final TestContainer t1 = TENANT.exec(TestTenants.DEFAULT, obj::get);
        final TestContainer t2 = TENANT.exec(TestTenants.DEFAULT, obj::get);
        
        assertSame(t1, t2);
    }
    
    /**
     * Tests that when using a {@link LazyMultiTenantObject} a different object is held per tenant. And the same object
     * is returned on subsequent calls
     */
    @Test
    public void getFromObject_tenantsDoNotShareObjects() {
        assertEquals("TESTdefault", TENANT.exec(TestTenants.DEFAULT, obj::get).get());
        assertEquals("TESTleft", TENANT.exec(TestTenants.LEFT, obj::get).get());
    }
    
    @Test
    public void unset_tenantsCleared() {
        final LazyMultiTenantObject<TestContainer> newobj = new TestLazyMultiTenantObject(multiTenancy, "TEST2");
        
        final TestContainer t1 = TENANT.exec(TestTenants.DEFAULT, newobj::get);
        final TestContainer t2 = TENANT.exec(TestTenants.DEFAULT, newobj::get);
        
        assertEquals("TEST2default", t1.get());
        assertSame(t1, t2);
        
        multiTenancy.stop();
        
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.addTenant(TestTenants.LEFT);
        multiTenancy.addTenant(TestTenants.RIGHT);
        
        final TestContainer t3 = TENANT.exec(TestTenants.DEFAULT, newobj::get);
        final TestContainer t4 = TENANT.exec(TestTenants.DEFAULT, newobj::get);
        
        assertEquals("TEST2default", t3.get());
        assertNotSame(t1, t3);
        assertSame(t3, t4);
    }
    
}
