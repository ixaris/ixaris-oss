/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractValueDef;
import com.ixaris.commons.dimensions.config.value.LongValue;
import com.ixaris.commons.dimensions.config.value.validation.MinCascadeValueValidation;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestValue4 extends AbstractValueDef<LongValue> {
    
    private static final TestValue4 INSTANCE = new TestValue4();
    
    public static TestValue4 getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_PROP4";
    
    private TestValue4() {
        super("Test Long Property", new ContextDef(PDimDef.getInstance()), new MinCascadeValueValidation<>());
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
