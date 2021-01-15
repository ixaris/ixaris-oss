package com.ixaris.commons.dimensions.limits.support;

import com.ixaris.commons.dimensions.lib.context.PartialContext;
import com.ixaris.commons.dimensions.lib.dimension.LongDimensionDef;

public final class Long2DimensionDef extends LongDimensionDef<Long> {
    
    private Range range = Range.MATCH_ANY;
    
    private static final Long2DimensionDef INSTANCE = new Long2DimensionDef();
    
    public static Long2DimensionDef getInstance() {
        return INSTANCE;
    }
    
    @Override
    public Range getRange() {
        return range;
    }
    
    public void setRange(final Range range) {
        this.range = range;
    }
    
    @Override
    public long convertNonNullValueToLong(final Long value) {
        return value;
    }
    
    @Override
    public Long getValueFromSpecificLong(final long l, final PartialContext context) {
        return l;
    }
    
    @Override
    public String getStringValue(final Long value) {
        return String.valueOf(value);
    }
    
    @Override
    public boolean isCacheable() {
        return false;
    }
}
