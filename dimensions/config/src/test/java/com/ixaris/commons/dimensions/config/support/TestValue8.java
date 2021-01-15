/*
 * Copyright 2002, 2014 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import static com.ixaris.commons.dimensions.lib.context.ContextDimensionDef.of;

import com.ixaris.commons.dimensions.config.AbstractValueDef;
import com.ixaris.commons.dimensions.config.value.LongValue;
import com.ixaris.commons.dimensions.lib.base.DimensionalDef;
import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.dimensions.lib.context.DimensionConfigRequirement;

public final class TestValue8 extends AbstractValueDef<LongValue> {
    
    private static final TestValue8 INSTANCE = new TestValue8();
    
    public static TestValue8 getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_PROP8";
    
    public TestValue8() {
        super("Test Long Property with REQUIRED dimension",
            new ContextDef(of(ADimDef.getInstance(), DimensionConfigRequirement.REQUIRED, true)),
            null);
    }
    
    /**
     * @see DimensionalDef#getKey()
     */
    @Override
    public String getKey() {
        return KEY;
    }
    
}
