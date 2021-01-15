package com.ixaris.commons.dimensions.config;

import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.config.value.validation.SetValidation;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public abstract class AbstractIncrementalSetDef<T extends Value> extends AbstractSetDef<T> {
    
    protected AbstractIncrementalSetDef(final String description,
                                        final ContextDef contextDef,
                                        final SetValidation<? super T> validation,
                                        final boolean cacheable) {
        
        super(description, contextDef, true, validation, cacheable);
    }
    
    @Override
    public boolean isNullExpected() {
        return true;
    }
    
}
