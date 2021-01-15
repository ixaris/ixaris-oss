/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.lib.context.NotComparableException;
import com.ixaris.commons.dimensions.lib.context.PartialContext;
import com.ixaris.commons.dimensions.lib.context.PersistedDimensionValue;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchyNode;

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
public abstract class StringHierarchyDimensionDef<T> extends AbstractDimensionDef<T> {
    
    /**
     * The constant that means "The current value for this dimension".
     *
     * <p>Dimensions are ordered in DESCending order when querying, such that NULLS come after defined values since these are less important.
     * This value should ideally be ordered to appear after any possible defined value for this dimension, since this is less important than an
     * exact match. However it is more important than if the dimension was not defined in the first place, so cannot be defined as NULL.
     *
     * <p>Thus, this value starts with a space, which, when ordered in DESCending order, places last after all other printable characters.
     */
    public static final String MATCH_ANY = " !current";
    
    public StringHierarchyDimensionDef() {
        super();
    }
    
    public StringHierarchyDimensionDef(final String key) {
        super(key);
    }
    
    protected abstract T getValueFromSpecificString(String str, PartialContext context);
    
    protected abstract String convertNonNullValueToString(final T value);
    
    @Override
    public final String getStringValue(final T value) {
        return value == null ? MATCH_ANY : convertNonNullValueToString(value);
    }
    
    @Override
    public final ValueMatch getValueMatch() {
        return (isMatchAnySupported() || isHierarchical()) ? ValueMatch.STRING_HIERARCHY : ValueMatch.STRING;
    }
    
    @Override
    public Dimension<T> create(final T value) {
        if (value == null) {
            if (!isMatchAnySupported()) {
                throw new IllegalArgumentException("definition does not support MATCH_ANY");
            }
        } else if (isHierarchical() && !(value instanceof HierarchyNode)) {
            throw new IllegalArgumentException("value is not hierarchical");
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
        
        return create(getValue(persistedValue.getStringValue(), context));
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
        
        return create(getValue(stringValue, context));
    }
    
    @Override
    public final PersistedDimensionValue getPersistedValue(final T value) {
        return new PersistedDimensionValue(getStringValue(value));
    }
    
    @Override
    protected final int compareNonNullNonEqualValues(final T v1, final T v2) throws NotComparableException {
        if (isHierarchical()) {
            final HierarchyNode<?> firstNode = (HierarchyNode<?>) v1;
            final HierarchyNode<?> secondNode = (HierarchyNode<?>) v2;
            
            // loop until the other node is found, or we arrive at the root (meaning they are not related)
            int distance = 0;
            HierarchyNode<?> t = firstNode;
            while (t != null) {
                if (t.equals(secondNode)) {
                    return distance;
                }
                t = t.getParent();
                distance++;
            }
            
            // unroll the first iteration since otherNode can never be equal to thisNode (checked above)
            distance = -1;
            HierarchyNode<?> o = secondNode.getParent();
            while (o != null) {
                if (o.equals(firstNode)) {
                    return distance;
                }
                
                o = o.getParent();
                distance--;
            }
        }
        
        throw new NotComparableException();
    }
    
    @Override
    public boolean isMatchAnySupported() {
        return false;
    }
    
    /**
     * A Hierarchical dimension's values implement the interface {@link HierarchyNode}
     *
     * @return true for dimensions with hierarchical values
     */
    public boolean isHierarchical() {
        return false;
    }
    
    /**
     * @return the maximum depth excluding the root (root is at depth 0, so 0 is equivalent to no hierarchy)
     */
    public byte getMaxDepth() {
        return 6;
    }
    
    @Override
    public final byte getHashWidth() {
        if (isHierarchical()) {
            byte maxDepth = getMaxDepth();
            if ((maxDepth < 1) || (maxDepth > 125)) {
                throw new IllegalStateException("Illegal maxDepth [" + maxDepth + "]. Should be between 1 and 125");
            }
            maxDepth += isMatchAnySupported() ? 2 : 1; // add 1 for wildcard and 1 for match any if supported
            if (maxDepth < 4) {
                return 2;
            } else if (maxDepth < 8) {
                return 3;
            } else if (maxDepth < 16) {
                return 4;
            } else if (maxDepth < 32) {
                return 5;
            } else if (maxDepth < 64) {
                return 6;
            } else {
                return 7;
            }
        } else {
            return super.getHashWidth();
        }
    }
    
    @Override
    public final byte getHash(final T value) {
        if (isHierarchical()) {
            if (value == null) {
                return 1;
            }
            final byte depth = ((HierarchyNode<?>) value).getDepth();
            if ((depth < 0) || (depth > getMaxDepth())) {
                throw new IllegalStateException(String.format("Invalid depth [%d] for dimension [%s]. Should be between 0 and %d", depth, this, getMaxDepth()));
            }
            return (byte) (depth + (isMatchAnySupported() ? 2 : 1));
        } else {
            return super.getHash(value);
        }
    }
    
    private T getValue(final String stringValue, final PartialContext context) {
        return (isMatchAnySupported() && (MATCH_ANY.equals(stringValue))) ? null : getValueFromSpecificString(stringValue, context);
    }
    
}
