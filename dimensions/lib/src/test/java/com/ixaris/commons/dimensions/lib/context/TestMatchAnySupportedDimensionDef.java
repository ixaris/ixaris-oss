package com.ixaris.commons.dimensions.lib.context;

import com.ixaris.commons.dimensions.lib.dimension.StringDimensionDef;

/**
 * A Test {@link StringDimensionDef} that supports MATCH_ANY.
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 */
public class TestMatchAnySupportedDimensionDef extends StringDimensionDef<String> {
    
    private static final TestMatchAnySupportedDimensionDef INSTANCE = new TestMatchAnySupportedDimensionDef();
    
    public static TestMatchAnySupportedDimensionDef getInstance() {
        return INSTANCE;
    }
    
    private TestMatchAnySupportedDimensionDef() {}
    
    @Override
    public boolean isMatchAnySupported() {
        return true;
    }
    
    @Override
    protected String getValueFromSpecificString(final String str, final PartialContext context) {
        return str;
    }
    
    @Override
    protected String convertNonNullValueToString(final String value) {
        return value;
    }
    
    @Override
    public boolean isCacheable() {
        return false;
    }
}
