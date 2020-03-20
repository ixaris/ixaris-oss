package com.ixaris.commons.collections.lib;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;

public class ExtendedCollectors {
    
    public static <T, U> Collector<T, ?, Optional<U>> reducing(final Function<T, U> initiator, final BiFunction<U, T, U> op) {
        
        return reducing(initiator, op, (u1, u2) -> {
            throw new UnsupportedOperationException("To use with parallel streams, specify a combiner");
        });
    }
    
    public static <T, U> Collector<T, ?, Optional<U>> reducing(final Function<T, U> initiator,
                                                               final BiFunction<U, T, U> op,
                                                               final BinaryOperator<U> combiner) {
        
        class OptionalBox implements Consumer<T> {
            
            private U value;
            private boolean present;
            
            public OptionalBox() {}
            
            public OptionalBox(final U value) {
                this.value = value;
                present = value != null;
            }
            
            @Override
            public void accept(final T t) {
                if (present) {
                    value = op.apply(value, t);
                } else {
                    value = initiator.apply(t);
                    present = true;
                }
            }
        }
        
        return Collector.of(OptionalBox::new,
            OptionalBox::accept,
            (a, b) -> {
                if (b.present) {
                    if (a.present) {
                        return new OptionalBox(combiner.apply(a.value, b.value));
                    } else {
                        return b;
                    }
                } else {
                    return a;
                }
            },
            a -> Optional.ofNullable(a.value));
    }
    
    public static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }
    
    private ExtendedCollectors() {}
    
}
