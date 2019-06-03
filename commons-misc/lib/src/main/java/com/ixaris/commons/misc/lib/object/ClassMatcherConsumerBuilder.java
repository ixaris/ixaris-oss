package com.ixaris.commons.misc.lib.object;

import com.ixaris.commons.misc.lib.function.ConsumerThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import java.util.ArrayList;
import java.util.List;

public final class ClassMatcherConsumerBuilder<T, E extends Exception> {
    
    public static final <T, E extends Exception> ClassMatcherConsumerBuilder<T, E> newBuilder() {
        return new ClassMatcherConsumerBuilder<>();
    }
    
    private final List<CaseBlock<? extends T>> cases = new ArrayList<>();
    
    private ClassMatcherConsumerBuilder() {}
    
    @SafeVarargs
    public final <ST extends T> CaseBlock<ST> when(final Class<? extends ST>... matchClasses) {
        return new CaseBlock<>(matchClasses);
    }
    
    @SuppressWarnings("unchecked")
    public ConsumerThrows<T, E> otherwise(final ConsumerThrows<T, E> otherwise) {
        return t -> {
            for (final CaseBlock<? extends T> aCase : cases) {
                if (ClassUtil.isInstanceOf(t, aCase.matchClasses)) {
                    aCase.internalAccept(t);
                }
            }
            otherwise.accept(t);
        };
    }
    
    @SuppressWarnings("unchecked")
    public ConsumerThrows<T, E> otherwiseThrow(final FunctionThrows<T, E, E> otherwise) {
        return t -> {
            for (final CaseBlock<? extends T> aCase : cases) {
                if (ClassUtil.isInstanceOf(t, aCase.matchClasses)) {
                    aCase.internalAccept(t);
                }
            }
            throw otherwise.apply(t);
        };
    }
    
    public final class CaseBlock<ST extends T> {
        
        private final Class<? extends ST>[] matchClasses;
        private ConsumerThrows<? super ST, E> consumer;
        
        @SafeVarargs
        private CaseBlock(final Class<? extends ST>... matchClasses) {
            if (matchClasses == null) {
                throw new IllegalArgumentException("matchClasses is null");
            }
            this.matchClasses = matchClasses;
        }
        
        public ClassMatcherConsumerBuilder<T, E> accept(final ConsumerThrows<? super ST, E> consumer) {
            this.consumer = consumer;
            cases.add(this);
            return ClassMatcherConsumerBuilder.this;
        }
        
        @SuppressWarnings("unchecked")
        private void internalAccept(final T t) throws E {
            consumer.accept((ST) t);
        }
        
    }
    
}
