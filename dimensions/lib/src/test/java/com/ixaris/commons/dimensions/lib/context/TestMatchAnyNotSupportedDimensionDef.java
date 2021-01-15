package com.ixaris.commons.dimensions.lib.context;

import com.ixaris.commons.dimensions.lib.dimension.StringDimensionDef;

/**
 * A Test {@link StringDimensionDef} that <b>does not</b> support MATCH_ANY.
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 */
public class TestMatchAnyNotSupportedDimensionDef extends StringDimensionDef<String> {
    
    private static final TestMatchAnyNotSupportedDimensionDef INSTANCE = new TestMatchAnyNotSupportedDimensionDef();
    
    public static TestMatchAnyNotSupportedDimensionDef getInstance() {
        return INSTANCE;
    }
    
    private TestMatchAnyNotSupportedDimensionDef() {}
    
    @Override
    public boolean isMatchAnySupported() {
        return false;
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
