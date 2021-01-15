package com.ixaris.commons.dimensions.limits;

import com.ixaris.commons.dimensions.lib.base.AbstractDimensionalDef;
import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;

/**
 * Base class for property definitions
 *
 * @author <a href="mailto:brian.vella@ixaris.com">BV</a>
 */
public abstract class AbstractValueLimitDef<I extends AbstractLimitExtensionEntity<?, I>> extends AbstractDimensionalDef implements ValueLimitDef<I> {
    
    /**
     * @param description
     * @param contextDef
     */
    public AbstractValueLimitDef(final String description, final ContextDef contextDef) {
        super(description, contextDef);
    }
    
    @Override
    public String getFriendlyName() {
        return getClass().getSimpleName().replaceAll("ValueLimitDef", "");
    }
    
    @Override
    public final CommonsDimensionsLimits.ValueLimitDef toProtobuf() {
        return CommonsDimensionsLimits.ValueLimitDef.newBuilder()
            .setKey(getKey())
            .setName(getFriendlyName())
            .setDescription(getDescription())
            .setContextDef(getContextDef().toProtobuf())
            .setCriterion(getCriterion())
            .build();
    }
    
}
