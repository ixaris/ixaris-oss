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

public final class TestOptionalDimensionsLimit extends AbstractValueLimitDef<NoLimitExtensionEntity> {
    
    public static final String KEY = "TEST_OPTIONAL_LIMIT";
    
    private static final TestOptionalDimensionsLimit INSTANCE = new TestOptionalDimensionsLimit();
    
    private TestOptionalDimensionsLimit() {
        super("Test Limit", new ContextDef(Long1DimensionDef.getInstance(), Long2DimensionDef.getInstance()));
    }
    
    public static TestOptionalDimensionsLimit getInstance() {
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
