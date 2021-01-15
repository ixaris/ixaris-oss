package com.ixaris.commons.dimensions.lib.context;

import java.util.Map;

public final class ContextDimensionDef<T> implements DimensionDef<T> {
    
    public static <T> ContextDimensionDef<T> of(final DimensionDef<T> contextDimensionDef,
                                                final DimensionConfigRequirement configRequirement,
                                                final boolean requiredForQuery) {
        return new ContextDimensionDef<>(contextDimensionDef, configRequirement, requiredForQuery);
    }
    
    private final DimensionDef<T> dimensionDef;
    private final DimensionConfigRequirement configRequirement;
    private final boolean requiredForQuery;
    
    private ContextDimensionDef(final DimensionDef<T> dimensionDef,
                                final DimensionConfigRequirement configRequirement,
                                final boolean requiredForQuery) {
        if (dimensionDef == null) {
            throw new IllegalArgumentException("dimensionDef is null");
        }
        if (configRequirement == null) {
            throw new IllegalArgumentException("configRequirement is null");
        }
        
        this.dimensionDef = dimensionDef;
        this.configRequirement = configRequirement;
        this.requiredForQuery = requiredForQuery;
    }
    
    public DimensionDef<T> getDef() {
        return dimensionDef;
    }
    
    public DimensionConfigRequirement getConfigRequirement() {
        return configRequirement;
    }
    
    public boolean isRequiredForQuery() {
        return requiredForQuery;
    }
    
    @Override
    public String getKey() {
        return dimensionDef.getKey();
    }
    
    @Override
    public String getStringValue(final T value) {
        return dimensionDef.getStringValue(value);
    }
    
    @Override
    public PersistedDimensionValue getPersistedValue(final T value) {
        return dimensionDef.getPersistedValue(value);
    }
    
    @Override
    public Dimension<T> create(final T value) {
        return dimensionDef.create(value);
    }
    
    @Override
    public Dimension<T> createFromPersistedValue(final PersistedDimensionValue persistedValue, final PartialContext context) {
        return dimensionDef.createFromPersistedValue(persistedValue, context);
    }
    
    @Override
    public Dimension<T> createFromString(final String stringValue, final PartialContext context) {
        return dimensionDef.createFromString(stringValue, context);
    }
    
    @Override
    public void validate(final T value, final PartialContext context) throws ConfigValidationException {
        dimensionDef.validate(value, context);
    }
    
    @Override
    public boolean isFixedValue() {
        return dimensionDef.isFixedValue();
    }
    
    @Override
    public boolean isMatchAnySupported() {
        return dimensionDef.isMatchAnySupported();
    }
    
    @Override
    public Dimension<T> createMatchAnyDimension() {
        return dimensionDef.createMatchAnyDimension();
    }
    
    @Override
    public Map<String, String> getFixedValues() {
        return dimensionDef.getFixedValues();
    }
    
    @Override
    public boolean isCacheable() {
        return dimensionDef.isCacheable();
    }
    
    @Override
    public ValueMatch getValueMatch() {
        return dimensionDef.getValueMatch();
    }
    
    @Override
    public int compare(final T v1, final T v2) throws NotComparableException {
        return dimensionDef.compare(v1, v2);
    }
    
    @Override
    public byte getHashWidth() {
        return dimensionDef.getHashWidth();
    }
    
    @Override
    public byte getHash(final T value) {
        return dimensionDef.getHash(value);
    }
    
    @Override
    public String toString(final T value) {
        return dimensionDef.toString(value);
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o instanceof ContextDimensionDef) {
            return dimensionDef.equals(((ContextDimensionDef<?>) o).dimensionDef);
        } else {
            return dimensionDef.equals(o);
        }
    }
    
    @Override
    public int hashCode() {
        return dimensionDef.hashCode();
    }
    
    @Override
    public String toString() {
        return dimensionDef.getKey() + ":" + configRequirement.name();
    }
    
}
