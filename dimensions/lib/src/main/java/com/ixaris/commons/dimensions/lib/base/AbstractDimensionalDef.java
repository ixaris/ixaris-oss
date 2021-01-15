package com.ixaris.commons.dimensions.lib.base;

import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.misc.lib.registry.AbstractRegisterable;

/**
 * Base class for property definitions
 *
 * @author <a href="mailto:brian.vella@ixaris.com">BV</a>
 */
public abstract class AbstractDimensionalDef extends AbstractRegisterable implements DimensionalDef {
    
    private final String description;
    private final ContextDef contextDef;
    
    /**
     * @param description
     * @param contextDef
     */
    public AbstractDimensionalDef(final String description, final ContextDef contextDef) {
        if (contextDef == null) {
            throw new IllegalArgumentException("contextDef is null");
        }
        
        this.description = description;
        this.contextDef = contextDef;
    }
    
    @Override
    public final String getDescription() {
        return description;
    }
    
    /**
     * @return the definition of the supported context. Order is very important. May not be null
     */
    @Override
    public final ContextDef getContextDef() {
        return contextDef;
    }
    
    @Override
    public String getFriendlyName() {
        return getClass().getSimpleName().replaceAll("Def", "");
    }
    
}
