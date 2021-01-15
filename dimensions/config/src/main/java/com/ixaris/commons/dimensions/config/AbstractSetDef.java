package com.ixaris.commons.dimensions.config;

import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSetDef;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.config.value.validation.SetValidation;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

/**
 * Base class for set definitions
 *
 * @author <a href="mailto:brian.vella@ixaris.com">BV</a>
 */
public abstract class AbstractSetDef<T extends Value> extends AbstractConfigDef<T> implements SetDef<T> {
    
    private final SetValidation<? super T> validation;
    private final boolean cacheable;
    private final boolean incremental;
    
    protected AbstractSetDef(final String description,
                             final ContextDef contextDef,
                             final SetValidation<? super T> setValidation,
                             final boolean cacheable) {
        
        this(description, contextDef, false, setValidation, cacheable);
    }
    
    AbstractSetDef(final String description,
                   final ContextDef contextDef,
                   final boolean incremental,
                   final SetValidation<? super T> validation,
                   final boolean cacheable) {
        
        super(description, contextDef);
        
        this.incremental = incremental;
        this.validation = validation;
        this.cacheable = cacheable;
    }
    
    @Override
    public final SetValidation<? super T> getValidation() {
        return validation;
    }
    
    @Override
    public boolean isCacheable() {
        return cacheable;
    }
    
    @Override
    public boolean isIncremental() {
        return incremental;
    }
    
    @Override
    public String getFriendlyName() {
        return getClass().getSimpleName().replaceAll("SetDef", "");
    }
    
    @Override
    public ConfigSetDef toProtobuf() {
        return ConfigSetDef.newBuilder()
            .setKey(getKey())
            .setName(getFriendlyName())
            .setDescription(getDescription())
            .setContextDef(getContextDef().toProtobuf())
            .setIncremental(isIncremental())
            .addAllParts(Value.toProtobuf(getValueBuilder()))
            .build();
    }
    
}
