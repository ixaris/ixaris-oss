package com.ixaris.commons.dimensions.limits.support;

import com.ixaris.commons.dimensions.lib.context.PartialContext;
import com.ixaris.commons.dimensions.lib.dimension.StringDimensionDef;

public final class Test1DimensionDef extends StringDimensionDef<String> {
    
    private static boolean cacheable = false;
    
    private static final String CATCH_ALL = " !testCatchAll";
    
    private static Test1DimensionDef instance = new Test1DimensionDef();
    
    public static Test1DimensionDef getInstance() {
        return instance;
    }
    
    @Override
    protected String getValueFromSpecificString(final String str, final PartialContext context) {
        return getStringValue(str);
    }
    
    @Override
    protected String convertNonNullValueToString(final String value) {
        return value;
    }
    
    public static void setCacheable(final boolean cacheable) {
        Test1DimensionDef.cacheable = cacheable;
    }
    
    @Override
    public boolean isCacheable() {
        return cacheable;
    }
    
    @Override
    public boolean isMatchAnySupported() {
        return true;
    }
    
}
