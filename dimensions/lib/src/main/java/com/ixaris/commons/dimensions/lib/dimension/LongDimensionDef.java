/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.lib.context.NotComparableException;
import com.ixaris.commons.dimensions.lib.context.PartialContext;
import com.ixaris.commons.dimensions.lib.context.PersistedDimensionValue;

/**
 * This dimension may potentially match multiple values. Some possible scenarios are:
 *
 * <ul>
 *   <li>A dimension that is based on a hierarchy, where any parent to the current value may be matched
 *   <li>A dimension that matches the all values, where the catch all will be matched if no specific match for the current value is found
 *   <li>A dimension based on a function
 * </ul>
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public abstract class LongDimensionDef<T> extends AbstractDimensionDef<T> {
    
    public static final long MATCH_ANY = Long.MIN_VALUE;
    
    public enum Range {
        EXACT,
        MATCH_ANY,
        SMALLER,
        LARGER
    }
    
    protected abstract T getValueFromSpecificLong(long l, PartialContext context);
    
    protected abstract long convertNonNullValueToLong(T value);
    
    protected final long getLongValue(final T value) {
        return value == null ? MATCH_ANY : convertNonNullValueToLong(value);
    }
    
    @Override
    public boolean isMatchAnySupported() {
        return Range.MATCH_ANY.equals(getRange());
    }
    
    @Override
    public final ValueMatch getValueMatch() {
        return Range.EXACT.equals(getRange()) ? ValueMatch.LONG : ValueMatch.LONG_RANGE;
    }
    
    @Override
    public final Dimension<T> create(final T value) {
        if (!isMatchAnySupported() && (value == null)) {
            throw new IllegalArgumentException("definition does not support MATCH_ANY");
        }
        
        return new Dimension<>(value, this);
    }
    
    /**
     * Creates this dimension from a persisted value
     *
     * @param persistedValue
     * @param context
     */
    @Override
    public final Dimension<T> createFromPersistedValue(final PersistedDimensionValue persistedValue, final PartialContext context) {
        if (persistedValue == null) {
            throw new IllegalArgumentException("persistedValue is null");
        }
        if (persistedValue.getLongValue() == null) {
            throw new IllegalArgumentException("Long part is null");
        }
        
        return create(getValue(persistedValue.getLongValue(), context));
    }
    
    /**
     * Creates this dimension from a string
     *
     * @param stringValue
     * @param context
     */
    @Override
    public final Dimension<T> createFromString(final String stringValue, final PartialContext context) {
        if (stringValue == null) {
            throw new IllegalArgumentException("stringValue is null");
        }
        
        return create(getValue(Long.parseLong(stringValue), context));
    }
    
    @Override
    public final PersistedDimensionValue getPersistedValue(final T value) {
        return new PersistedDimensionValue(getLongValue(value));
    }
    
    @Override
    public final int compareNonNullNonEqualValues(final T v1, final T v2) throws NotComparableException {
        
        final long l1 = getLongValue(v1);
        final long l2 = getLongValue(v2);
        
        switch (getRange()) {
            case LARGER:
                return l1 < l2 ? 1 : -1; // no check for equal as this is already checked
            case SMALLER:
                return l1 > l2 ? 1 : -1; // no check for equal as this is already checked
        }
        
        throw new NotComparableException();
    }
    
    private T getValue(final long longValue, final PartialContext context) {
        return (isMatchAnySupported() && (longValue == MATCH_ANY)) ? null : getValueFromSpecificLong(longValue, context);
    }
    
    public Range getRange() {
        return Range.EXACT;
    }
    
}
