package com.ixaris.commons.dimensions.config;

import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValueDef;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.config.value.validation.ValueValidation;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

/**
 * Base class for property definitions
 *
 * @author <a href="mailto:brian.vella@ixaris.com">BV</a>
 */
public abstract class AbstractValueDef<T extends Value> extends AbstractConfigDef<T> implements ValueDef<T> {
    
    private final ValueValidation<? super T> validation;
    
    /**
     * @param description
     * @param contextDef
     * @param validation may be null
     */
    protected AbstractValueDef(final String description, final ContextDef contextDef, final ValueValidation<? super T> validation) {
        super(description, contextDef);
        this.validation = validation;
    }
    
    public AbstractValueDef(final String description, final ContextDef contextDef) {
        this(description, contextDef, null);
    }
    
    @Override
    public final ValueValidation<? super T> getValidation() {
        return validation;
    }
    
    @Override
    public boolean isDeletable() {
        return true;
    }
    
    @Override
    public String getFriendlyName() {
        return getClass().getSimpleName().replaceAll("ValueDef", "");
    }
    
    @Override
    public ConfigValueDef toProtobuf() {
        return ConfigValueDef.newBuilder()
            .setKey(getKey())
            .setName(getFriendlyName())
            .setDescription(getDescription())
            .setContextDef(getContextDef().toProtobuf())
            .setDeletable(isDeletable())
            .addAllParts(Value.toProtobuf(getValueBuilder()))
            .build();
    }
}
