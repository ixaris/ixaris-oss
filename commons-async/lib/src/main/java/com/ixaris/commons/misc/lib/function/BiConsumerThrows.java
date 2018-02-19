package com.ixaris.commons.misc.lib.function;

import java.util.function.BiConsumer;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;

@FunctionalInterface
public interface BiConsumerThrows<T, U, E extends Throwable> {
    
    static <T, U> BiConsumerThrows<T, U, RuntimeException> from(final BiConsumer<T, U> c) {
        return c::accept;
    }
    
    static <T, U> BiConsumer<T, U> asConsumer(final BiConsumerThrows<T, U, ?> consumer) {
        return (t, u) -> {
            try {
                consumer.accept(t, u);
            } catch (final Throwable e) { // NOSONAR catch throwable to rethrow without declaring
                throw ExceptionUtil.sneakyThrow(e);
            }
        };
    }
    
    void accept(T t, U u) throws E;
    
}
