/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import java.util.Map;
import java.util.Objects;

import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.lib.context.DimensionDef;
import com.ixaris.commons.dimensions.lib.context.NotComparableException;
import com.ixaris.commons.dimensions.lib.context.PartialContext;
import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Dimension Definition (singleton).
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
abstract class AbstractDimensionDef<T> implements DimensionDef<T> {
    
    private final String key;
    
    /**
     * Constructor
     */
    public AbstractDimensionDef() {
        key = DimensionDef.extractKey(getClass());
    }
    
    public AbstractDimensionDef(final String key) {
        if (key.length() > 50) {
            this.key = key.substring(0, 50);
        } else {
            this.key = key;
        }
    }
    
    /**
     * @return the name for this context dimension
     */
    public final String getKey() {
        return key;
    }
    
    /**
     * A dimension may require other dimensions, and it may need to validate itself against these required dimensions. Default behaviour is to
     * omit validation. One should check if the required dimensions are there.
     *
     * @param value
     * @param context
     */
    @Override
    public void validate(T value, PartialContext context) throws ConfigValidationException {}
    
    public boolean isFixedValue() {
        return false;
    }
    
    public boolean isMatchAnySupported() {
        return false;
    }
    
    @Override
    public final Dimension<T> createMatchAnyDimension() {
        if (!isMatchAnySupported()) {
            throw new IllegalStateException("MATCH_ANY value not supported.");
        }
        
        return new Dimension<>(null, this);
    }
    
    /**
     * Get the enumerated list of possible values for this dimension. Default implementation assumes the dimension is not enumerable. This
     * representation should follow the getStringValue() / createFromString() representation
     *
     * @return the enumerated list
     * @throws UnsupportedOperationException if the dimension is not enumerable
     */
    public Map<String, String> getFixedValues() {
        throw new UnsupportedOperationException("Dimension not enumerable");
    }
    
    public final int compare(final T v1, final T v2) throws NotComparableException {
        
        if (Objects.equals(v1, v2)) {
            return 0;
        }
        
        if (isMatchAnySupported()) {
            if (v1 == null) {
                // if this dimension is catch all, the other is 
                // - either catch all as well (equal) which we already checked for
                // - or something other than catch all, which would not match, therefore -1
                return -1;
                
            } else if (v2 == null) {
                // this is the farthest we can go from the value, but it is still implied
                return 1;
            }
        }
        
        return compareNonNullNonEqualValues(v1, v2);
    }
    
    /**
     * Override if the dimension definition can support a hierarchy of values, to compare the non-null, non-equal values in the hierarchy. E.g.
     * if A has children B and C, A should be < B and C (less specific), but B and C are not comparable
     *
     * @param v1
     * @param v2
     * @return positibe if v1 is mor specific than v2, negative otherwise. Should not return 0 as these values are not equal
     * @throws NotComparableException if the values are not part of the same hierarchy
     */
    protected int compareNonNullNonEqualValues(final T v1, final T v2) throws NotComparableException {
        throw new NotComparableException();
    }
    
    /**
     * Determine the width (in bits) of the specif level. Default is 1 meaning it either applies or not. Hierarchical dimensions will need a
     * wider walue, depending on the max depth. E.g. a max depth of 7 requires 3 bits
     *
     * @return the specific level width
     */
    public byte getHashWidth() {
        if (isMatchAnySupported()) {
            return 2;
        } else {
            return 1;
        }
    }
    
    /**
     * The hash determines how specific this dimension is. The default assumes a width of 1 and return the maximum (1).
     *
     * <p>For hierarchical dimensions, this may vary from 2 (least specific) to X (most specific). It is recommended that the maximum depth is
     * kept to either 6 (requiring 3 bits) or 14 (requiring 4 bits)
     *
     * <p>0 is reserved for a non-defined dimension and should never be returned, and 1 is reserved for catch all values
     *
     * @param value
     * @return the specificity
     */
    public byte getHash(final T value) {
        if (isMatchAnySupported()) {
            if (value == null) {
                return 1;
            }
            return 2;
        } else {
            return 1;
        }
    }
    
    public String toString(final T value) {
        return getKey() + "=" + value;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> key.equals(other.key));
    }
    
    @Override
    public int hashCode() {
        return key.hashCode();
    }
    
    @Override
    public String toString() {
        return getKey();
    }
    
}
