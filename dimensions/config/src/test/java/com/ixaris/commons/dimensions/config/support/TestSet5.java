/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractIncrementalSetDef;
import com.ixaris.commons.dimensions.config.value.StringValue;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestSet5 extends AbstractIncrementalSetDef<StringValue> {
    
    public static final String KEY = "TEST_SET5";
    
    private static final TestSet5 INSTANCE = new TestSet5();
    
    public static TestSet5 getInstance() {
        return INSTANCE;
    }
    
    private TestSet5() {
        super("Test String Property", new ContextDef(ADimDef.getInstance(), BDimDef.getInstance(), CDimDef.getInstance()), null, true); // non-cacheable
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
