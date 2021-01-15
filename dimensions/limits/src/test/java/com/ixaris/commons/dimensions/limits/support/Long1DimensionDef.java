package com.ixaris.commons.dimensions.limits.support;

import com.ixaris.commons.dimensions.lib.context.PartialContext;
import com.ixaris.commons.dimensions.lib.dimension.LongDimensionDef;

public final class Long1DimensionDef extends LongDimensionDef<Long> {
    
    private Range range = Range.MATCH_ANY;
    
    private static final Long1DimensionDef INSTANCE = new Long1DimensionDef();
    
    public static Long1DimensionDef getInstance() {
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
