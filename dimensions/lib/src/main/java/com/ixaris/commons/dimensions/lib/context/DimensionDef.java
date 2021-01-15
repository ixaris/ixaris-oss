/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.context;

import java.util.Map;

/**
 * Dimension Definition (singleton).
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public interface DimensionDef<T> {
    
    static String extractKey(final Class<?> defClass) {
        final String tentativeKey = defClass.getSimpleName().replaceFirst("Dim(:?ension)?Def", "");
        if (tentativeKey.length() > 60) {
            return tentativeKey.substring(0, 60);
        } else {
            return tentativeKey;
        }
    }
    
    enum ValueMatch {
        
        LONG,
        
        /**
         * These dimensions may match more than one value. The value is a long, but the ordering is done on a comparison (< or >), the closer the
         * number, the more specific the dimension is (or the closer the context is)
         */
        LONG_RANGE,
        
        STRING,
        
        /**
         * These dimensions may match more than one value. The value is a string, but the ordering is done on a hierarchy qualifier (the depth),
         * the deeper the item, the more specific the dimension is (or the closer the context is)
         */
        STRING_HIERARCHY;
        
    }
    
    String getKey();
    
    /**
     * Passing this string back to createFromString() method should recreate the same dimension. Primarily used for UI. Can show a different
     * representation (e.g. a converted value) as long as the contract above is respected, i.e. passing this value to createFromString()
     * recreates the same dimension.
     *
     * @param value
     * @return the String representation for this value
     */
    String getStringValue(T value);
    
    /**
     * @param value
     * @return the persistedValue for this value
     */
    PersistedDimensionValue getPersistedValue(T value);
    
    /**
     * Create this dimension
     *
     * @param value
     */
    Dimension<T> create(T value);
    
    /**
     * Create this dimension from a persisted value
     *
     * @param persistedValue
     * @param context
     */
    Dimension<T> createFromPersistedValue(PersistedDimensionValue persistedValue, PartialContext context);
    
    /**
     * Create this dimension from a string
     *
     * @param stringValue
     * @param context
     */
    Dimension<T> createFromString(String stringValue, PartialContext context);
    
    /**
     * A dimension may require other dimensions, and it may need to validate itself against these required dimensions. Default behaviour is to
     * omit validation. One should check if the required dimensions are there.
     *
     * @param value
     * @param context
     * @throws ConfigValidationException if the dimension is not valid
     */
    void validate(T value, PartialContext context) throws ConfigValidationException;
    
    /**
     * Indicates that the dimension has a finite list representation. Default implementation assumes the dimension is not enumerable.
     *
     * @return true if this dimension is enumerable, false otherwise
     */
    boolean isFixedValue();
    
    /**
     * Catch all is used in the context where there is a difference in semantics to the wildcard value (null). Aggregate limits would be
     * candidates for this difference. Consider a limit per type.
     *
     * <ul>
     *   <li>configuring the limit as [type = *] means you want a limit on the aggregate of all types
     *   <li>configuring the limit as [type = MATCH_ANY] means you want a limit on the aggregate per type, for all types
     *   <li>configuring the limit as [type = SPECIFIC_TYPE] means you want a limit on the aggregate of a specific type
     * </ul>
     *
     * In this case the match any configuration would be overridden by the specific type configuration if this is set.
     *
     * @return true if catch all is supported by this dimension
     */
    boolean isMatchAnySupported();
    
    /**
     * @return a {@link Dimension} representing a MATCH_ANY value. Use this method when MATCH_ANY is supported by the context dimension and an
     *     exact value is not available.
     * @throws IllegalStateException if MATCH_ANY is not supported by this dimension (i.e. if {@link DimensionDef#isMatchAnySupported()} is
     *     {@literal false}.
     */
    Dimension<T> createMatchAnyDimension();
    
    /**
     * Get the enumerated list of possible values for this dimension. Default implementation assumes the dimension is not enumerable. This
     * representation should follow the getStringValue() / createFromString() representation
     *
     * @return the enumerated list
     */
    Map<String, String> getFixedValues();
    
    /**
     * Not all contextual property values are cacheable
     *
     * <p>Contextual property values that are specific to certain context dimensions (e.g. properties that vary by the an id context) are likely
     * to have a low cache-hit probability
     *
     * <p>If this is such a context dimension, we mark it as uncacheable. Consequently, if a Context contains one or more such dimensions, values
     * for that context instance will be cached separately, to keep more cacheable items from being evicted due to cache churn.
     *
     * @return true if we want to cache items that vary by this context dimension, false otherwise.
     */
    boolean isCacheable();
    
    /**
     * Indicates how to match values for this dimension. Possible scenarios are:
     *
     * <ul>
     *   <li>a simple string / long / decimal value
     *   <li>A dimension that is based on a hierarchy, where any parent to the current value may be matched
     *   <li>A dimension that matches all values, where the catch all will be matched if no specific match for the current value is found
     *   <li>A dimension based on a function?
     * </ul>
     *
     * Default implementation is false.
     *
     * @return true if this dimension can match current value, false otherwise
     */
    ValueMatch getValueMatch();
    
    /**
     * Compare specificity of 2 values
     *
     * <p>The result should be:
     *
     * <ul>
     *   <li>positive if the first value is more specific than the second, i.e. is implied but not exactly equal to the second value. The larger
     *       the number, the less specific the second value is to the first
     *   <li>negative if the second value is more specific than the first (opposite of the above)
     *   <li>0 if they are exactly equal
     *   <li>
     * </ul>
     *
     * @return the distance as defined above
     * @throws NotComparableException
     */
    @SuppressWarnings("unchecked")
    int compare(final T v1, final T v2) throws NotComparableException;
    
    /**
     * Determine the width (in bits) of the specif level. Default is 1 meaning it either applies or not. Hierarchical dimensions will need a
     * wider walue, depending on the max depth. E.g. a max depth of 7 requires 3 bits
     *
     * @return the specific level width
     */
    byte getHashWidth();
    
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
    byte getHash(final T value);
    
    String toString(final T value);
    
}
