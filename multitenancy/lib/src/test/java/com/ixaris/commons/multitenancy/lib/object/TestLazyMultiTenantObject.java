package com.ixaris.commons.multitenancy.lib.object;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.object.TestLazyMultiTenantObject.TestContainer;

/**
 * @author <a href="mailto:matthias.portelli@ixaris.com">matthias.portelli</a>
 */
public final class TestLazyMultiTenantObject extends LazyMultiTenantObject<TestContainer> {
    
    public static final class TestContainer {
        
        private final String s;
        
        public TestContainer(final String s) {
            this.s = s;
        }
        
        public String get() {
            return s;
        }
    }
    
    public TestLazyMultiTenantObject(final MultiTenancy multiTenancy, final String s) {
        super(multiTenancy, () -> new TestContainer(s + MultiTenancy.getCurrentTenant()));
    }
    
}
