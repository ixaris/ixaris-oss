/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits.support;

import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.dimensions.limits.AbstractValueLimitDef;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity.Fetch;
import com.ixaris.commons.dimensions.limits.data.NoLimitExtensionEntity;

public final class TestComparatorLimit extends AbstractValueLimitDef<NoLimitExtensionEntity> {
    
    public static final String KEY = "TEST_COMPARATOR_LIMIT";
    
    private static final TestComparatorLimit INSTANCE = new TestComparatorLimit();
    
    private TestComparatorLimit() {
        super("Test Comparator Limit", new ContextDef(Test1DimensionDef.getInstance(), Test2DimensionDef.getInstance()));
    }
    
    public static TestComparatorLimit getInstance() {
        return INSTANCE;
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
    @Override
    public String getFriendlyName() {
        return null;
    }
    
    @Override
    public LimitCriterion getCriterion() {
        return LimitCriterion.COUNT_MIN_MAX;
    }
    
    @Override
    public Fetch<NoLimitExtensionEntity, Void> getInfoFetch() {
        return NoLimitExtensionEntity.FETCH;
    }
    
}
