/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.context;

import java.util.Objects;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

/**
 * Persisted dimension value. All dimensions values should be able to reduce to a long or string, and be created from these values. These values
 * are what is stored in the DB.
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class PersistedDimensionValue {
    
    private Long longValue = null;
    private String stringValue = null;
    
    public PersistedDimensionValue(final Long longValue, final String stringValue) {
        
        if ((longValue == null) && (stringValue == null)) {
            throw new IllegalArgumentException("All values are null!");
        }
        if ((longValue != null) && (stringValue != null)) {
            throw new IllegalArgumentException("All values are set! Only one of long or string can be set");
        }
        
        this.longValue = longValue;
        this.stringValue = stringValue;
    }
    
    public PersistedDimensionValue(final long longValue) {
        this.longValue = longValue;
        this.stringValue = null;
    }
    
    public PersistedDimensionValue(final String stringValue) {
        if (stringValue == null) {
            throw new IllegalArgumentException("stringValue is null");
        }
        
        this.longValue = null;
        this.stringValue = stringValue;
    }
    
    public final Long getLongValue() {
        return longValue;
    }
    
    public final String getStringValue() {
        return stringValue;
    }
    
    @Override
    public final boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> Objects.equals(longValue, other.longValue) && Objects.equals(stringValue, other.stringValue));
    }
    
    @Override
    public final int hashCode() {
        return Objects.hash(longValue, stringValue);
    }
    
    @Override
    public final String toString() {
        return ToStringUtil.of(this).with("longValue", longValue).with("stringValue", stringValue).toString();
    }
}
