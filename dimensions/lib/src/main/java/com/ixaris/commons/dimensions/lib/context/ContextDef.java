/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.context;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ixaris.commons.dimensions.lib.CommonsDimensionsLib;
import com.ixaris.commons.dimensions.lib.CommonsDimensionsLib.DimensionDef.ConfigRequirement;
import com.ixaris.commons.dimensions.lib.CommonsDimensionsLib.KeyName;
import com.ixaris.commons.dimensions.lib.base.DimensionalDef;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

/**
 * A property's supported context. This is an ordered list of supported dimensions useful to eliminate ambiguous context value selection for a
 * particular context.
 *
 * <p>Consider a property supporting context dimensions A and B and the following configuration
 *
 * <table border>
 * <tr>
 * <th>property
 * <th>A
 * <th>B
 * <th>value
 * <tr>
 * <td>prop
 * <td>Va
 * <td>*
 * <td>10
 * <tr>
 * <td>prop
 * <td>*
 * <td>Vb
 * <td>20
 * </table>
 *
 * Which value should be selected for context A=Va&B=Vb?
 *
 * <p>If supported context dimensions is A,B meaning A is more important than B, then 10 would be chosen, otherwise, for B,A 20 would be chosen
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public final class ContextDef implements Iterable<DimensionDef<?>> {
    
    public static final class SupportedDimensionDetails implements Serializable {
        
        private static final long serialVersionUID = -7741138973086177158L;
        
        private final DimensionConfigRequirement configRequirement;
        private final boolean requiredForQuery;
        private final byte shift;
        
        public SupportedDimensionDetails(final DimensionConfigRequirement configRequirement, final boolean requiredForQuery, final byte shift) {
            
            this.configRequirement = configRequirement;
            this.requiredForQuery = requiredForQuery;
            this.shift = shift;
        }
        
        public DimensionConfigRequirement getConfigRequirement() {
            return configRequirement;
        }
        
        public boolean isRequiredForQuery() {
            return requiredForQuery;
        }
        
        public int getImportance() {
            return shift;
        }
        
    }
    
    private static final class ByteContainer {
        
        private byte value;
        
        private ByteContainer(final byte value) {
            this.value = value;
        }
        
    }
    
    /**
     * holds a set of supported context dimensions, ordered from most important to last important.
     *
     * <p>LinkedHashSet is used to guarantee the order of the supported contexts
     */
    private final Map<DimensionDef<?>, SupportedDimensionDetails> orderedSupportedDimensions;
    
    /**
     * Map to aid in resolving dimension name to actual dimension class
     */
    private final Map<String, DimensionDef<?>> dimensionResolutionMap;
    
    /**
     * Constructor for an empty supported context
     */
    public ContextDef() {
        this(Collections.emptyList());
    }
    
    /**
     * Constructor. Order of supported dimensions is very important
     *
     * @param orderedSupportedDimensionArray the supported dimensions, ordered from most important to least important
     */
    public ContextDef(final DimensionDef<?>... orderedSupportedDimensionArray) {
        this(Arrays.asList(orderedSupportedDimensionArray));
    }
    
    public ContextDef(final Collection<DimensionDef<?>> orderedSupportedDimensionList) {
        // we require a Linked hash set to retain the insertion order, which is equivalent to the order of importance
        final LinkedHashMap<DimensionDef<?>, SupportedDimensionDetails> importanceMap = new LinkedHashMap<>(orderedSupportedDimensionList
            .size());
        final Map<String, DimensionDef<?>> resolutionMap = new HashMap<>(orderedSupportedDimensionList.size());
        
        // We limit the context width to 63 bits since the 64th bit turns the number negative.
        // We need this number for sorting contexts based on how specific they are, where more specific > less specific
        // Thus we want to avoid a more specific context turning negative and breaking this rule
        final ByteContainer shift = new ByteContainer((byte) 63);
        for (DimensionDef<?> supportedDimension : orderedSupportedDimensionList) {
            if (supportedDimension == null) {
                throw new IllegalArgumentException("orderedSupportedDimensionList contains nulls");
            }
            addDimension(supportedDimension, importanceMap, resolutionMap, shift);
        }
        
        orderedSupportedDimensions = Collections.unmodifiableMap(importanceMap);
        dimensionResolutionMap = Collections.unmodifiableMap(resolutionMap);
    }
    
    private void addDimension(final DimensionDef<?> dimensionDef,
                              final LinkedHashMap<DimensionDef<?>, SupportedDimensionDetails> importanceMap,
                              final Map<String, DimensionDef<?>> resolutionMap,
                              final ByteContainer shift) {
        if (dimensionDef instanceof ContextDimensionDef) {
            final ContextDimensionDef<?> contextDimensionDef = (ContextDimensionDef<?>) dimensionDef;
            addDimension(contextDimensionDef.getDef(),
                contextDimensionDef.getConfigRequirement(),
                contextDimensionDef.isRequiredForQuery(),
                importanceMap,
                resolutionMap,
                shift);
        } else {
            addDimension(dimensionDef, DimensionConfigRequirement.OPTIONAL, false, importanceMap, resolutionMap, shift);
        }
    }
    
    private void addDimension(final DimensionDef<?> supportedDimension,
                              final DimensionConfigRequirement dimensionConfigRequirement,
                              final boolean requiredForQuery,
                              final LinkedHashMap<DimensionDef<?>, SupportedDimensionDetails> importanceMap,
                              final Map<String, DimensionDef<?>> resolutionMap,
                              final ByteContainer shift) {
        if (importanceMap.containsKey(supportedDimension)) {
            throw new IllegalStateException("supported dimension " + supportedDimension + " has already been added");
        }
        
        shift.value -= supportedDimension.getHashWidth();
        if (shift.value < 0) {
            throw new IllegalArgumentException("Total hash width exceeds 63 bits");
        }
        importanceMap.put(supportedDimension, new SupportedDimensionDetails(dimensionConfigRequirement, requiredForQuery, shift.value));
        resolutionMap.put(supportedDimension.getKey(), supportedDimension);
    }
    
    /**
     * @return the number of supported context dimensions
     */
    public int size() {
        return orderedSupportedDimensions.size();
    }
    
    /**
     * @param dimension
     * @return true if the given context dimension is supported
     */
    public boolean isDimensionSupported(final DimensionDef<?> dimension) {
        return orderedSupportedDimensions.containsKey(dimension);
    }
    
    public DimensionConfigRequirement getSupportedDimensionType(final DimensionDef<?> dimension) {
        final SupportedDimensionDetails details = orderedSupportedDimensions.get(dimension);
        return (details != null) ? details.configRequirement : null;
    }
    
    public byte getShift(final DimensionDef<?> dimension) {
        final SupportedDimensionDetails details = orderedSupportedDimensions.get(dimension);
        return (details != null) ? details.shift : -1;
    }
    
    /**
     * @param dimensionKey
     * @return the resolved context dimension class
     */
    public DimensionDef<?> resolve(final String dimensionKey) {
        DimensionDef<?> resolvedDimensionDef = dimensionResolutionMap.get(dimensionKey);
        
        if (resolvedDimensionDef == null) {
            throw new IllegalStateException("Dimension definition [" + dimensionKey + "] cannot be resolved in context definition [" + this + "]");
        } else {
            return resolvedDimensionDef;
        }
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("orderedSupportedDimensions", orderedSupportedDimensions.keySet()).toString();
    }
    
    /**
     * @return an iterator over the supported dimensions, in order of importance from most to least important
     */
    public Map<DimensionDef<?>, SupportedDimensionDetails> getMap() {
        return orderedSupportedDimensions;
    }
    
    public boolean isEmpty() {
        return orderedSupportedDimensions.keySet().isEmpty();
    }
    
    public boolean contains(final DimensionDef<?> o) {
        return orderedSupportedDimensions.keySet().contains(o);
    }
    
    @Override
    public Iterator<DimensionDef<?>> iterator() {
        return orderedSupportedDimensions.keySet().iterator();
    }
    
    public CommonsDimensionsLib.ContextDef toProtobuf() {
        if (isEmpty()) {
            return CommonsDimensionsLib.ContextDef.getDefaultInstance();
        }
        final CommonsDimensionsLib.ContextDef.Builder ctxBuilder = CommonsDimensionsLib.ContextDef.newBuilder();
        for (final Entry<DimensionDef<?>, SupportedDimensionDetails> dimension : orderedSupportedDimensions.entrySet()) {
            final DimensionDef<?> def = dimension.getKey();
            final SupportedDimensionDetails conf = dimension.getValue();
            final CommonsDimensionsLib.DimensionDef.Builder dimBuilder = CommonsDimensionsLib.DimensionDef.newBuilder()
                .setKey(def.getKey())
                .setName(def.getKey())
                .setMatchAnySupported(def.isMatchAnySupported())
                .setConfigRequirement(ConfigRequirement.valueOf(conf.configRequirement.name()))
                .setRequiredForQuery(conf.requiredForQuery);
            
            if (def.isFixedValue()) {
                for (final Entry<String, String> value : def.getFixedValues().entrySet()) {
                    dimBuilder.addFixedValues(KeyName.newBuilder().setKey(value.getKey()).setName(value.getValue()));
                }
            }
            ctxBuilder.addDimensions(dimBuilder.build());
        }
        return ctxBuilder.build();
    }
    
    public <D extends DimensionalDef> Context<D> contextFromProtobuf(final D dimensionalDef, final CommonsDimensionsLib.Context fromContext) {
        if (fromContext.getDimensionsCount() == 0) {
            return Context.empty(dimensionalDef);
        }
        final Context.Builder<D> ctxBuilder = Context.newBuilder(dimensionalDef);
        for (final CommonsDimensionsLib.Dimension dimension : fromContext.getDimensionsList()) {
            final DimensionDef<?> def = resolve(dimension.getKey());
            if (dimension.getMatchAny()) {
                ctxBuilder.add(def.createMatchAnyDimension());
            } else if (dimension.getValue() != null) {
                ctxBuilder.add(def.createFromString(dimension.getValue(), ctxBuilder));
            }
        }
        return ctxBuilder.build();
    }
    
}
