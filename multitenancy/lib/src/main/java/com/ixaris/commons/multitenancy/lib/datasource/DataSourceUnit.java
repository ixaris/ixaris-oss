package com.ixaris.commons.multitenancy.lib.datasource;

import com.ixaris.commons.multitenancy.lib.data.DataUnit;

/**
 * TODO set DataUnit to final when this is removed
 *
 * @deprecated Use {@link DataUnit}
 */
@Deprecated
public final class DataSourceUnit implements DataUnit {
    
    private final String unit;
    
    public DataSourceUnit(final String unit) {
        this.unit = unit;
    }
    
    @Override
    public String get() {
        return unit;
    }
    
}
