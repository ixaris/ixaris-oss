package com.ixaris.commons.dimensions.limits.support;

import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.dimensions.limits.AbstractCounterLimitDef;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity.Fetch;
import com.ixaris.commons.dimensions.limits.data.NoLimitExtensionEntity;

public final class TestAggregateLimit extends AbstractCounterLimitDef<NoLimitExtensionEntity, TestCounterDef> {
    
    public static final String KEY = "TEST_AGGREGATE";
    
    private static final TestAggregateLimit INSTANCE = new TestAggregateLimit();
    
    private TestAggregateLimit() {
        super("Test Aggregate Limit", new ContextDef(Long1DimensionDef.getInstance(), Long2DimensionDef.getInstance()));
    }
    
    public static TestAggregateLimit getInstance() {
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
    
    @Override
    public TestCounterDef getCounterDef() {
        return TestCounterDef.getInstance();
    }
    
}
