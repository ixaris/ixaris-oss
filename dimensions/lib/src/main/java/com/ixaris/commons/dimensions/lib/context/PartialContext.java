package com.ixaris.commons.dimensions.lib.context;

public interface PartialContext {
    
    /**
     * @param dimensionDef The context dimension definition to check
     * @return true if a dimension is defined, false otherwise
     */
    boolean isDimensionDefined(DimensionDef<?> dimensionDef);
    
    /**
     * @param dimensionDef The context dimension definition to check
     * @param withSpecificValue true to check if dimension is defined with a specific value, not catch all
     * @return true if a dimension is defined, false otherwise
     */
    <T> boolean isDimensionDefined(final DimensionDef<T> dimensionDef, final boolean withSpecificValue);
    
    /**
     * @param dimensionDef The definition of the context dimension class retrieve
     * @return the context dimension matching the given dimension class, or null if none defined
     */
    <T> Dimension<T> getDimension(final DimensionDef<T> dimensionDef);
    
}
