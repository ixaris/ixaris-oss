package com.ixaris.commons.dimensions.config.value.validation;

import java.util.Set;

import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;
import com.ixaris.commons.dimensions.lib.context.Context;

public abstract class ContextValidation<T extends Value> implements ValueValidation<T>, SetValidation<T> {
    
    /**
     * Validates the value of a context property value.
     *
     * <p>Should not assume that value is of the required type. Type checking is a must
     *
     * @param value the value to check
     * @param context
     * @throws ConfigValidationException if the value is invalid
     */
    public abstract <V extends T> void validate(V value, Context context) throws ConfigValidationException;
    
    /**
     * Validates the value of a context property value.
     *
     * <p>Should not assume that value is of the required type. Type checking is a must
     *
     * @param set the set of values to check
     * @param context
     * @throws ConfigValidationException if a value is invalid
     */
    public <V extends T> void validate(final Set<V> set, final Context context) throws ConfigValidationException {
        for (V value : set) {
            validate(value, context);
        }
    }
    
    @Override
    public final Type getType() {
        return Type.CONTEXT;
    }
    
}
