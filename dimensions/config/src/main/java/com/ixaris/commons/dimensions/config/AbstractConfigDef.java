package com.ixaris.commons.dimensions.config;

import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.base.AbstractDimensionalDef;
import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.misc.lib.object.GenericsUtil;

/**
 * Base class for property definitions
 *
 * @author <a href="mailto:brian.vella@ixaris.com">BV</a>
 */
public abstract class AbstractConfigDef<T extends Value> extends AbstractDimensionalDef implements ConfigDef<T> {
    
    private final Class<T> valueType;
    
    /**
     * @param description
     * @param contextDef
     */
    @SuppressWarnings("unchecked")
    public AbstractConfigDef(final String description, final ContextDef contextDef) {
        super(description, contextDef);
        this.valueType = (Class<T>) GenericsUtil.getGenericTypeArguments(getClass(), AbstractConfigDef.class).get("T");
    }
    
    @Override
    public final Class<T> getType() {
        return valueType;
    }
    
    @Override
    public Value.Builder<T> getValueBuilder() {
        return Value.getBuilderForType(valueType);
    }
    
    @Override
    public boolean isNullExpected() {
        return false;
    }
    
}
