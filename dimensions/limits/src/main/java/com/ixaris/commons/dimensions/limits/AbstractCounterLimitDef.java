package com.ixaris.commons.dimensions.limits;

import java.util.Collections;
import java.util.Set;

import com.ixaris.commons.dimensions.counters.CounterDef;
import com.ixaris.commons.dimensions.lib.base.AbstractDimensionalDef;
import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;

/**
 * Base class for property definitions
 *
 * @author <a href="mailto:brian.vella@ixaris.com">BV</a>
 */
public abstract class AbstractCounterLimitDef<I extends AbstractLimitExtensionEntity<?, I>, C extends CounterDef<?, C>> extends AbstractDimensionalDef implements CounterLimitDef<I, C> {
    
    /**
     * @param description
     * @param contextDef
     */
    public AbstractCounterLimitDef(final String description, final ContextDef contextDef) {
        super(description, contextDef);
    }
    
    @Override
    public Set<Dimension<?>> getConstantDimensions() {
        return Collections.emptySet();
    }
    
    @Override
    public String getFriendlyName() {
        return getClass().getSimpleName().replaceAll("CounterLimitDef", "");
    }
    
    @Override
    public final CommonsDimensionsLimits.CounterLimitDef toProtobuf() {
        final CommonsDimensionsLimits.CounterLimitDef.Builder builder = CommonsDimensionsLimits.CounterLimitDef.newBuilder()
            .setKey(getKey())
            .setName(getFriendlyName())
            .setDescription(getDescription())
            .setContextDef(getContextDef().toProtobuf())
            .setCriterion(getCriterion());
        
        for (Dimension<?> dimension : getConstantDimensions()) {
            builder.addConstantDimensions(dimension.toProtobuf());
        }
        
        return builder.build();
    }
    
}
