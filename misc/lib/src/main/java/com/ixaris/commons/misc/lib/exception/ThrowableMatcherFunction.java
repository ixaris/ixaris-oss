package com.ixaris.commons.misc.lib.exception;

import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.object.ClassUtil;

public final class ThrowableMatcherFunction<T extends Throwable, R> {
    
    public static <T extends Throwable, R> ThrowableMatcherFunction<T, R> match(final T instance) {
        return new ThrowableMatcherFunction<>(instance);
    }
    
    private final T instance;
    private boolean matched;
    private R result;
    
    private ThrowableMatcherFunction(final T instance) {
        this.instance = instance;
    }
    
    @SuppressWarnings("unchecked")
    public <ST extends T, E extends Throwable> ThrowableMatcherFunction<T, R> when(final Class<ST> matchClass,
                                                                                   final FunctionThrows<ST, R, E> function) throws E {
        return new CaseBlock<>(matchClass).apply(function);
    }
    
    @SuppressWarnings("unchecked")
    public <ST extends T, E extends Throwable> ThrowableMatcherFunction<T, R> throwWhen(final Class<ST> matchClass,
                                                                                        final FunctionThrows<ST, E, E> function) throws E {
        return new CaseBlock<>(matchClass).throwApply(function);
    }
    
    @SafeVarargs
    public final <ST extends T> CaseBlock<ST> when(final Class<? extends ST>... matchClasses) {
        return new CaseBlock<>(matchClasses);
    }
    
    @SuppressWarnings("unchecked")
    public <ST extends T, E extends Throwable> R otherwise(final FunctionThrows<ST, R, E> function) throws E {
        if (!matched) {
            matched = true;
            result = function.apply((ST) instance);
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public <ST extends T, E extends Throwable> R throwOtherwise(final FunctionThrows<ST, E, E> function) throws E {
        if (!matched) {
            matched = true;
            throw function.apply((ST) instance);
        }
        return result;
    }
    
    public R throwUnmatched() throws T {
        if (!matched) {
            matched = true;
            throw instance;
        }
        return result;
    }
    
    public R get() {
        return result;
    }
    
    public T unwrap() {
        return instance;
    }
    
    public final class CaseBlock<ST extends T> {
        
        private final Class<? extends ST>[] matchClasses;
        
        @SafeVarargs
        private CaseBlock(final Class<? extends ST>... matchClasses) {
            if (matchClasses == null) {
                throw new IllegalArgumentException("matchClasses is null");
            }
            this.matchClasses = matchClasses;
        }
        
        @SuppressWarnings("unchecked")
        public <E extends Throwable> ThrowableMatcherFunction<T, R> apply(final FunctionThrows<? super ST, R, E> function) throws E {
            
            if (!matched && ClassUtil.isInstanceOf(instance, matchClasses)) {
                matched = true;
                result = function.apply((ST) instance);
            }
            return ThrowableMatcherFunction.this;
        }
        
        @SuppressWarnings("unchecked")
        public <E extends Throwable> ThrowableMatcherFunction<T, R> throwApply(final FunctionThrows<? super ST, E, E> function) throws E {
            
            if (!matched && ClassUtil.isInstanceOf(instance, matchClasses)) {
                matched = true;
                throw function.apply((ST) instance);
            }
            return ThrowableMatcherFunction.this;
        }
        
    }
    
}
