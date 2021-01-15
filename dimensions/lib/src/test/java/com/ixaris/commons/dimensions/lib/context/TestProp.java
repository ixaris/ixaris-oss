/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.context;

import com.ixaris.commons.dimensions.lib.base.AbstractDimensionalDef;

public class TestProp extends AbstractDimensionalDef {
    
    private static final TestProp INSTANCE = new TestProp();
    
    public static TestProp getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_PROP1";
    
    private TestProp() {
        super("Test String Property",
            new ContextDef(TestMatchAnySupportedDimensionDef.getInstance(), TestMatchAnyNotSupportedDimensionDef.getInstance()));
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
    @Override
    public String getFriendlyName() {
        return KEY;
    }
}
