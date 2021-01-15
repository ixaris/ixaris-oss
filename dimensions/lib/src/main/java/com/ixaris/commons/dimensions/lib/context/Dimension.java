/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.context;

import java.util.Objects;
import java.util.function.Function;

import com.ixaris.commons.dimensions.lib.CommonsDimensionsLib;
import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Base Context Dimension Class.
 *
 * <p>
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public final class Dimension<T> {
    
    public static <T> T valueOrNull(final Dimension<T> dimension) {
        return dimension != null ? dimension.getValue() : null;
    }
    
    public static <T, U> U mapValueOrNull(final Dimension<T> dimension, Function<T, U> valueFunction) {
        return dimension != null ? valueFunction.apply(dimension.getValue()) : null;
    }
    
    private final DimensionDef<T> definition;
    private final T value;
    
    public Dimension(final T value, final DimensionDef<T> definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition is null");
        }
        
        this.value = value;
        this.definition = definition;
    }
    
    /**
     * @return
     */
    public DimensionDef<T> getDefinition() {
        return definition;
    }
    
    /**
     * @return true if match any (value is null)
     */
    public boolean isMatchAny() {
        return value == null;
    }
    
    /**
     * @return the value or null if match any
     */
    public T getValue() {
        return value;
    }
    
    /**
     * @return the persisted value
     */
    public final PersistedDimensionValue getPersistedValue() {
        return getDefinition().getPersistedValue(value);
    }
    
    /**
     * @return the string representation of the value, never null.
     */
    public final String getStringValue() {
        return getDefinition().getStringValue(value);
    }
    
    /**
     * A dimension may require other dimensions, and it may need to validate itself against these required dimensions. Default behaviour is to
     * omit validation. One should check if the required dimensions are there.
     *
     * @param context
     * @throws ConfigValidationException ERR_VALUE_VALIDATION_FAILED if the dimension is not valid
     */
    public final void validate(final PartialContext context) throws ConfigValidationException {
        definition.validate(value, context);
    }
    
    public byte getHash() {
        return definition.getHash(value);
    }
    
    public CommonsDimensionsLib.Dimension toProtobuf() {
        final CommonsDimensionsLib.Dimension.Builder builder = CommonsDimensionsLib.Dimension.newBuilder().setKey(definition.getKey());
        
        if (isMatchAny()) {
            builder.setMatchAny(true);
        } else {
            builder.setValue(getStringValue());
        }
        
        return builder.build();
    }
    
    @Override
    public final boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> definition.equals(other.definition) && Objects.equals(value, other.value));
    }
    
    @Override
    public final int hashCode() {
        return Objects.hash(definition, value);
    }
    
    @Override
    public final String toString() {
        return definition.toString(value);
    }
    
    /**
     * Compare this context dimension to another context dimension
     *
     * <p>The result should be:
     *
     * <ul>
     *   <li>negative if this context instance is not equal to the given context dimension
     *   <li>0 if it is exactly equal to the given context dimension
     *   <li>positive if this context instance is implied but not exactly equal to the other context instance. The larger the number, the less
     *       specific the context dimension is to this dimension
     *   <li>
     * </ul>
     *
     * @param other
     * @return the distance as defined above
     * @throws NotComparableException
     */
    final int compareTo(final Dimension<T> other) throws NotComparableException {
        if (other == this) {
            return 0;
        } else {
            return definition.compare(value, other.value);
        }
    }
}
