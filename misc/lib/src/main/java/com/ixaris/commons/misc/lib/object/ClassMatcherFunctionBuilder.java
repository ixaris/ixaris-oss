package com.ixaris.commons.misc.lib.object;

import java.util.ArrayList;
import java.util.List;

import com.ixaris.commons.misc.lib.function.FunctionThrows;

public final class ClassMatcherFunctionBuilder<T, R, E extends Exception> {
    
    public static final <T, R, E extends Exception> ClassMatcherFunctionBuilder<T, R, E> newBuilder() {
        return new ClassMatcherFunctionBuilder<>();
    }
    
    private final List<CaseBlock<? extends T>> cases = new ArrayList<>();
    
    private ClassMatcherFunctionBuilder() {}
    
    @SafeVarargs
    public final <ST extends T> CaseBlock<ST> when(final Class<? extends ST>... matchClasses) {
        return new CaseBlock<>(matchClasses);
    }
    
    @SuppressWarnings("unchecked")
    public FunctionThrows<T, R, E> otherwise(final FunctionThrows<T, R, E> otherwise) {
        return t -> {
            for (final CaseBlock<? extends T> aCase : cases) {
                if (ClassUtil.isInstanceOf(t, aCase.matchClasses)) {
                    return aCase.internalApply(t);
                }
            }
            return otherwise.apply(t);
        };
    }
    
    @SuppressWarnings("unchecked")
    public FunctionThrows<T, R, E> otherwiseThrow(final FunctionThrows<T, E, E> otherwise) {
        return t -> {
            for (final CaseBlock<? extends T> aCase : cases) {
                if (ClassUtil.isInstanceOf(t, aCase.matchClasses)) {
                    return aCase.internalApply(t);
                }
            }
            throw otherwise.apply(t);
        };
    }
    
    public final class CaseBlock<ST extends T> {
        
        private final Class<? extends ST>[] matchClasses;
        private FunctionThrows<? super ST, R, E> function;
        
        @SafeVarargs
        private CaseBlock(final Class<? extends ST>... matchClasses) {
            if (matchClasses == null) {
                throw new IllegalArgumentException("matchClasses is null");
            }
            this.matchClasses = matchClasses;
        }
        
        public ClassMatcherFunctionBuilder<T, R, E> apply(final FunctionThrows<? super ST, R, E> function) {
            this.function = function;
            cases.add(this);
            return ClassMatcherFunctionBuilder.this;
        }
        
        @SuppressWarnings("unchecked")
        private R internalApply(final T t) throws E {
            return function.apply((ST) t);
        }
        
    }
    
}
