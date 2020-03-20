package com.ixaris.commons.misc.lib.exception;

import com.ixaris.commons.misc.lib.function.ConsumerThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.object.ClassUtil;

public final class ThrowableMatcherConsumer<T extends Throwable> {
    
    public static <T extends Throwable> ThrowableMatcherConsumer<T> match(final T instance) {
        return new ThrowableMatcherConsumer<>(instance);
    }
    
    private final T instance;
    private boolean matched;
    
    private ThrowableMatcherConsumer(final T instance) {
        this.instance = instance;
    }
    
    @SuppressWarnings("unchecked")
    public <ST extends T, E extends Throwable> ThrowableMatcherConsumer<T> when(final Class<ST> matchClass, final ConsumerThrows<ST, E> consumer) throws E {
        return new CaseBlock<>(matchClass).accept(consumer);
    }
    
    @SuppressWarnings("unchecked")
    public <ST extends T, E extends Throwable> ThrowableMatcherConsumer<T> throwWhen(final Class<ST> matchClass,
                                                                                     final FunctionThrows<ST, E, E> function) throws E {
        return new CaseBlock<>(matchClass).throwApply(function);
    }
    
    @SafeVarargs
    public final <ST extends T> CaseBlock<ST> when(final Class<? extends ST>... matchClasses) {
        return new CaseBlock<>(matchClasses);
    }
    
    @SuppressWarnings("unchecked")
    public <ST extends T, E extends Throwable> void otherwise(final ConsumerThrows<ST, E> consumer) throws E {
        if (!matched) {
            matched = true;
            consumer.accept((ST) instance);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <ST extends T, E extends Throwable> void throwOtherwise(final FunctionThrows<ST, E, E> function) throws E {
        if (!matched) {
            matched = true;
            throw function.apply((ST) instance);
        }
    }
    
    public void throwUnmatched() throws T {
        if (!matched) {
            matched = true;
            throw instance;
        }
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
        public <E extends Throwable> ThrowableMatcherConsumer<T> accept(final ConsumerThrows<? super ST, E> consumer) throws E {
            
            if (!matched && ClassUtil.isInstanceOf(instance, matchClasses)) {
                matched = true;
                consumer.accept((ST) instance);
            }
            return ThrowableMatcherConsumer.this;
        }
        
        @SuppressWarnings("unchecked")
        public <E extends Throwable> ThrowableMatcherConsumer<T> throwApply(final FunctionThrows<? super ST, E, E> function) throws E {
            
            if (!matched && ClassUtil.isInstanceOf(instance, matchClasses)) {
                matched = true;
                throw function.apply((ST) instance);
            }
            return ThrowableMatcherConsumer.this;
        }
        
    }
    
}
