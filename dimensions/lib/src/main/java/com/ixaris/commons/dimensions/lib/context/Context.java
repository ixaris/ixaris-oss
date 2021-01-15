/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.context;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ixaris.commons.dimensions.lib.CommonsDimensionsLib;
import com.ixaris.commons.dimensions.lib.base.DimensionalDef;
import com.ixaris.commons.dimensions.lib.context.ContextDef.SupportedDimensionDetails;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

/**
 * A context instance.
 *
 * <p>This is made up of a set of context dimension values, at a point in time. E.g. USER_STATE=Suspended AND USER_ID=joesmith123
 * AFFILIATE_ID=Bet365 AFFILIATE_ID=Bet365 AND CURRENCY_CODE=GBP AND SPEND_SINK_TYPE=DIRECT
 *
 * <p>Context Dimensions should be set in line with a property's supported context dimensions.
 */
public final class Context<D extends DimensionalDef> implements PartialContext {
    
    /**
     * Checks if the given context contains the given query context
     */
    public static <D extends DimensionalDef> boolean isContaining(final D def, final Context<D> context, final Context<D> queryContext) {
        try {
            for (final DimensionDef<?> keyDimension : def.getContextDef()) {
                // if limit dimension is less specific than the query dimension, fail the check
                if (DimensionalDef.compare(keyDimension, context, queryContext) < 0) {
                    return false;
                }
            }
            return true;
        } catch (final NotComparableException e) {
            return false;
        }
    }
    
    /**
     * Checks if the given context matches the given query context
     */
    public static <D extends DimensionalDef> boolean isMatching(final D def, final Context<D> context, final Context<D> queryContext) {
        try {
            for (final DimensionDef<?> keyDimension : def.getContextDef()) {
                // if limit dimension is more specific than the query dimension, fail the check
                if (DimensionalDef.compare(keyDimension, context, queryContext) > 0) {
                    return false;
                }
            }
            return true;
        } catch (final NotComparableException e) {
            return false;
        }
    }
    
    public static <X extends DimensionalDef> Context<X> empty(final X dimensionalDef) {
        return new Context<>(dimensionalDef, new LinkedHashMap<>(), 0L, 0L, true, true);
    }
    
    public static <X extends DimensionalDef> Builder<X> newBuilder(final X dimensionalDef) {
        return new Builder<>(dimensionalDef);
    }
    
    public static final class Builder<D extends DimensionalDef> implements PartialContext {
        
        private final D dimensionalDef;
        private LinkedHashMap<DimensionDef<?>, Dimension<?>> contextMap;
        private boolean cacheable; // Determines if values for this context instance may be cached. Some dimensions are too wide (e.g.
        // userId) to cache
        private boolean validForQuery;
        private long ctxDepth;
        private long dimMask;
        
        public Builder(final D dimensionalDef) {
            if (dimensionalDef == null) {
                throw new IllegalArgumentException("dimensionalDef is null");
            }
            
            this.dimensionalDef = dimensionalDef;
            reset();
        }
        
        @Override
        public boolean isDimensionDefined(final DimensionDef<?> dimensionDef) {
            return isDimensionDefined(dimensionDef, false);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> boolean isDimensionDefined(final DimensionDef<T> dimensionDef, final boolean withSpecificValue) {
            final Dimension<T> dimension = (Dimension<T>) contextMap.get(dimensionDef);
            return (dimension != null) && (!withSpecificValue || (dimension.getValue() != null));
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> Dimension<T> getDimension(final DimensionDef<T> dimensionDef) {
            return (Dimension<T>) contextMap.get(dimensionDef);
        }
        
        /**
         * A generic method for setting a Dimension value.
         *
         * @param dimension The context dimension. Cannot be null.
         * @return the previous context dimension instance, or null if no previous instance was set
         */
        @SuppressWarnings("unchecked")
        public Builder<D> add(final Dimension<?> dimension) {
            addInternal(dimension);
            return this;
        }
        
        /**
         * Adds a dimension to the context map
         *
         * @param dimension the dimension to be added
         */
        private void addInternal(final Dimension<?> dimension) {
            if (dimension == null) {
                throw new IllegalArgumentException("dimension is null");
            }
            
            final DimensionDef<?> dimensionDef = dimension.getDefinition();
            
            if (!dimensionalDef.getContextDef().isDimensionSupported(dimensionDef)) {
                // ignore
                return;
            }
            
            if (!contextMap.containsKey(dimensionDef)) {
                try {
                    dimension.validate(this);
                } catch (final ConfigValidationException e) {
                    throw new IllegalArgumentException(e);
                }
                if (!dimensionDef.isCacheable()) {
                    cacheable = false;
                }
            }
            
            final Dimension<?> oldDimension = contextMap.put(dimensionDef, dimension);
            if (oldDimension != null) {
                removeDimensionHash(oldDimension);
            }
            
            byte shift = dimensionalDef.getContextDef().getShift(dimensionDef);
            long dimensionMask = ((long) dimension.getHash()) << shift;
            // we or (|) the dimension's hash
            ctxDepth |= dimensionMask;
            dimMask |= 1 << shift;
            
            if (dimension.getValue() == null) {
                validForQuery = false;
            }
        }
        
        public Builder<D> addAll(final Dimension<?>... dimensions) {
            return addAll(Arrays.asList(dimensions));
        }
        
        /**
         * A generic method for setting a collection of Dimension value.
         *
         * @param dimensions The collection of context dimensions. Cannot be null.
         */
        @SuppressWarnings("unchecked")
        public Builder<D> addAll(final Collection<Dimension<?>> dimensions) {
            addAllInternal(dimensions);
            return this;
        }
        
        /**
         * Adds all the given context dimensions and optionally validates the dimensions when adding
         *
         * @param dimensions The dimensions to be added
         */
        private void addAllInternal(final Collection<Dimension<?>> dimensions) {
            if (dimensions == null) {
                throw new IllegalArgumentException("dimensions is null");
            }
            
            for (final Dimension<?> dimension : dimensions) {
                addInternal(dimension);
            }
        }
        
        /**
         * A generic method for removing a Dimension value.
         *
         * @param definition The dimension to be removed. Cannot be null.
         * @return the removed dimension instance, or null if no previous instance was set
         */
        @SuppressWarnings("unchecked")
        public Builder<D> remove(final DimensionDef<?> definition) {
            removeInternal(definition);
            return this;
        }
        
        /**
         * Removes a dimension with teh given definition from the context instance
         *
         * @param definition The type of dimension to remove
         * @throws IllegalArgumentException if the definition is null
         * @throws IllegalStateException if the context is already sealed
         */
        private void removeInternal(final DimensionDef<?> definition) {
            if (definition == null) {
                throw new IllegalArgumentException("definition is null");
            }
            
            // check if we already have the value set for this context element - throw a warning if we had already set a
            // value
            final Dimension<?> removedDimension = contextMap.remove(definition);
            
            // recalculate if the instance is cacheable
            // if context instance is already cacheable, this removal will not affect cacheability
            // if this dimension is cacheable, this removeal will not affect cacheability
            // if both instance and dimension are not cacheable, this removal may change cacheability
            if (removedDimension != null) {
                removeDimensionHash(removedDimension);
                
                if (!cacheable && !removedDimension.getDefinition().isCacheable()) {
                    cacheable = true;
                    // recheck all dimensions
                    for (final Dimension<?> dimension : contextMap.values()) {
                        if (!dimension.getDefinition().isCacheable()) {
                            cacheable = false;
                            break;
                        }
                    }
                }
            }
        }
        
        /**
         * Initiases a context instance from a map of Key:Value pairs which are relatable to context dimensions within the dimensionalDef's
         * supported context
         *
         * @param kvMap The map of key:value dimensions as strings
         */
        public Builder<D> from(final Map<String, PersistedDimensionValue> kvMap) {
            if (kvMap != null && !kvMap.isEmpty()) {
                for (final DimensionDef<?> dimensionDef : dimensionalDef.getContextDef()) {
                    final PersistedDimensionValue contextValue = kvMap.get(dimensionDef.getKey());
                    if (contextValue != null) {
                        // try adding the context to the context instance
                        final Dimension<?> dimension = dimensionDef.createFromPersistedValue(contextValue, this);
                        addInternal(dimension);
                    }
                }
            }
            
            return this;
        }
        
        public Context<D> build() {
            final Context<D> instance = new Context<>(dimensionalDef, contextMap, ctxDepth, dimMask, cacheable, validForQuery);
            reset();
            return instance;
        }
        
        void reset() {
            contextMap = new LinkedHashMap<>();
            cacheable = true;
            validForQuery = true;
            ctxDepth = 0L;
            dimMask = 0L;
        }
        
        private void removeDimensionHash(final Dimension<?> removedDimension) {
            byte shift = dimensionalDef.getContextDef().getShift(removedDimension.getDefinition());
            // we need to remove the old dimension's hash
            long dimensionMask = ((long) removedDimension.getHash()) << shift;
            // we and (&) the bitwise negation (~) of the dimension's hash
            ctxDepth &= ~dimensionMask;
            dimMask &= ~(1 << shift);
        }
        
    }
    
    private final D dimensionalDef;
    
    /**
     * Map backing the context instance. This maps between the dimension class and the instance providing
     *
     * <ul>
     *   <li>a quick lookup for a particular context dimension
     *   <li>keeping track of all dimension instances
     *   <li>keeping track of all dimensions
     * </ul>
     */
    private final LinkedHashMap<DimensionDef<?>, Dimension<?>> contextMap;
    
    private final Map<DimensionDef<?>, Dimension<?>> unmodifiableMap;
    
    /**
     * depth computed from the dimensions
     */
    private final long ctxDepth;
    
    private final long dimMask;
    
    /**
     * Determines if values for this context instance may be cached. Some dimensions are too vast (e.g. userId) to cache
     */
    private final boolean cacheable;
    
    /**
     * Determines if this context instance contains dimensions that are set with a value that is not valid for querying, or in other words,
     * values that should only be used when configuring.
     */
    private final boolean validForQuery;
    
    /**
     * A hash computed on the values
     */
    private volatile int valuesHash;
    
    private Context(final D dimensionalDef,
                    final LinkedHashMap<DimensionDef<?>, Dimension<?>> contextMap,
                    final long ctxDepth,
                    final long dimMask,
                    final boolean cacheable,
                    final boolean validForQuery) {
        this.dimensionalDef = dimensionalDef;
        this.contextMap = contextMap;
        unmodifiableMap = Collections.unmodifiableMap(contextMap);
        this.ctxDepth = ctxDepth;
        this.dimMask = dimMask;
        this.cacheable = cacheable;
        this.validForQuery = validForQuery;
    }
    
    public D getDef() {
        return dimensionalDef;
    }
    
    /**
     * @return if the context instance has no dimensions or not
     */
    public boolean isEmpty() {
        return contextMap.isEmpty();
    }
    
    /**
     * @return the number of supported context dimensions
     */
    public int size() {
        return contextMap.size();
    }
    
    /**
     * @param dimensionDef The context dimension definition to check
     * @return true if the dimension is defined (has a value or is match any)
     */
    @Override
    public boolean isDimensionDefined(final DimensionDef<?> dimensionDef) {
        return isDimensionDefined(dimensionDef, false);
    }
    
    /**
     * @param dimensionDef The context dimension definition to check
     * @param withSpecificValue true if a specific value is required, false if match-any value is considered as well
     * @return true if the dimension is defined
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> boolean isDimensionDefined(final DimensionDef<T> dimensionDef, final boolean withSpecificValue) {
        final Dimension<T> dimension = (Dimension<T>) contextMap.get(dimensionDef);
        return (dimension != null) && (!withSpecificValue || (dimension.getValue() != null));
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> Dimension<T> getDimension(final DimensionDef<T> dimensionDef) {
        return (Dimension<T>) contextMap.get(dimensionDef);
    }
    
    /**
     * @return the collection of context dimension instances defined
     */
    public Collection<Dimension<?>> getDimensions() {
        return Collections.unmodifiableCollection(contextMap.values());
    }
    
    /**
     * Extracts a ContextDef from the Context.
     *
     * @return The context definiton of this context
     */
    public ContextDef extractContext() {
        return new ContextDef(contextMap.keySet());
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> {
            if (!dimensionalDef.equals(other.dimensionalDef)) {
                return false;
            }
            if (hashCode() != other.hashCode()) {
                return false;
            }
            if (contextMap.size() != other.contextMap.size()) {
                return false;
            }
            for (final Entry<DimensionDef<?>, Dimension<?>> e : unmodifiableMap.entrySet()) {
                if (!e.getValue().equals(other.get(e.getKey()))) {
                    return false;
                }
            }
            return true;
        });
    }
    
    @Override
    public int hashCode() {
        if (valuesHash == 0) {
            // no need for synchronisation as valuesHash is volatile
            int hash = 1;
            for (final Dimension<?> dimension : contextMap.values()) {
                hash = 31 * hash + dimension.hashCode();
            }
            if (hash == 0) {
                hash = 1; // just in case hashcode ends up 0, we avoid recomputing everytime
            }
            valuesHash = hash;
        }
        return valuesHash;
    }
    
    @SuppressWarnings("unchecked")
    public <T> Dimension<T> get(final DimensionDef<T> key) {
        return (Dimension<T>) contextMap.get(key);
    }
    
    public Collection<Dimension<?>> values() {
        return unmodifiableMap.values();
    }
    
    /**
     * Not all contextual property values are cached.
     *
     * <p>If this context instance contains non-cacheable dimensions, we mark it, so any caching mechanism will know it should not cache values
     * for this context instance.
     *
     * @return true if we do not want to cache value that vary by this context instance, false otherwise.
     */
    public boolean isCacheable() {
        return cacheable;
    }
    
    /**
     * @return true if all dimension are valid for querying and all required dimensions are present, false otherwise
     */
    public boolean isValidForQuery() {
        if (!validForQuery) {
            return false;
        } else {
            for (final Entry<DimensionDef<?>, SupportedDimensionDetails> dimension : dimensionalDef.getContextDef().getMap().entrySet()) {
                // already checked for MATCH_ANY in if (!validForQuery) above
                if (dimension.getValue().isRequiredForQuery() && !isDimensionDefined(dimension.getKey())) {
                    return false;
                }
            }
            return true;
        }
    }
    
    /**
     * @param lookup true if performing a lookup
     * @return true if all required dimensions are present, false otherwise
     */
    public boolean isValidForConfig(final boolean lookup) {
        for (Map.Entry<DimensionDef<?>, SupportedDimensionDetails> dimension : dimensionalDef.getContextDef().getMap().entrySet()) {
            switch (dimension.getValue().getConfigRequirement()) {
                case UNDEFINED_OR_MATCH_ANY:
                    if (isDimensionDefined(dimension.getKey(), true)) {
                        return false;
                    }
                    break;
                case REQUIRED:
                    // if this dimension is required for config and it is not defined, them this context instance is not
                    // valid
                    if (!lookup && !isDimensionDefined(dimension.getKey())) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("dimensionalDef", dimensionalDef).with("dimensions", contextMap.values()).toString();
    }
    
    /**
     * @return the depth for this context, indicating how specific a context is (relative to other context instances). Takes into account
     *     hierarchies / catch all
     */
    public long getDepth() {
        return ctxDepth;
    }
    
    /**
     * Calculate a hash for the context of this context instance.
     *
     * @return the hash for this instance, indicating how specific a context is (relative to other context instances). Takes into account only
     *     defined dimensions
     */
    public long getDimensionsMask() {
        return dimMask;
    }
    
    /**
     * Check if a context instance is more specific that another
     *
     * @param otherContext
     * @return positive if this context instance is more specific than the parameter, 0 if they are equal and negative if less specific
     * @throws NotComparableException if the context instances have a common dimension that is not equal
     */
    public int compareTo(final Context<D> otherContext) throws NotComparableException {
        int result = 0;
        for (final DimensionDef<?> def : dimensionalDef.getContextDef()) {
            final int d = compareDimension(def, otherContext);
            if (d != 0) {
                // we record the first non-zero result only (the most important) but compare the whole context
                if (result == 0) {
                    result = d;
                }
            }
        }
        
        // if we get to here, it means the context instances are equal
        return result;
    }
    
    public CommonsDimensionsLib.Context toProtobuf() {
        if (isEmpty()) {
            return CommonsDimensionsLib.Context.getDefaultInstance();
        }
        final CommonsDimensionsLib.Context.Builder ctxBuilder = CommonsDimensionsLib.Context.newBuilder();
        for (final Dimension<?> dimension : contextMap.values()) {
            ctxBuilder.addDimensions(dimension.toProtobuf());
        }
        return ctxBuilder.build();
    }
    
    private <T> int compareDimension(final DimensionDef<T> def, final Context<D> otherContext) throws NotComparableException {
        
        final Dimension<T> dimension = getDimension(def);
        final Dimension<T> otherDimension = otherContext.getDimension(def);
        
        // if this dimension is defined, proceed
        if (dimension != null) {
            if (otherDimension == null) {
                return 1;
            } else {
                // if the dimension in the context being matched is defined, it should match, otherwise this context
                // does not apply
                return dimension.compareTo(otherDimension);
            }
        } else if (otherDimension != null) {
            // other
            return -1;
        } else {
            // both null
            return 0;
        }
    }
    
}
