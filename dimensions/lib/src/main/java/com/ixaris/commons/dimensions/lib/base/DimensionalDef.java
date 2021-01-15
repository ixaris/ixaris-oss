/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.base;

import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.lib.context.DimensionDef;
import com.ixaris.commons.dimensions.lib.context.NotComparableException;
import com.ixaris.commons.misc.lib.registry.NoRegistry;
import com.ixaris.commons.misc.lib.registry.Registerable;

/**
 * Interface to be defined by property definitions
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
@NoRegistry
public interface DimensionalDef extends Registerable {
    
    /**
     * Compare specificity of a dimension in 2 contexts
     *
     * <p>The result should be:
     *
     * <ul>
     *   <li>positive if the first dimension is more specific than the second, i.e. is implied but not exactly equal to the second dimension. The
     *       larger the number, the less specific the second dimension is to the first
     *   <li>negative if the second dimension is more specific than the first (opposite of the above)
     *   <li>0 if they are exactly equal
     *   <li>
     * </ul>
     *
     * @param dimensionDef
     * @param context1
     * @param context2
     * @param <T>
     * @return the distance as defined above
     * @throws NotComparableException
     */
    static <T> int compare(final DimensionDef<T> dimensionDef, final Context<?> context1, final Context<?> context2) throws NotComparableException {
        
        final Dimension<T> dimension1 = context1.get(dimensionDef);
        final Dimension<T> dimension2 = context2.get(dimensionDef);
        
        if (dimension1 == null) {
            if (dimension2 == null) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (dimension2 == null) {
                return 1;
            } else {
                return dimensionDef.compare(dimension1.getValue(), dimension2.getValue());
            }
        }
    }
    
    /**
     * @return the description of the property. Used for administration purposes.
     */
    String getDescription();
    
    /**
     * @return the definition of the supported context. Order is very important. May not be null
     */
    ContextDef getContextDef();
    
    /**
     * @return A friendly representation of this definition for UI purposes
     */
    String getFriendlyName();
    
}
