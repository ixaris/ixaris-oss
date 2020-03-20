package com.ixaris.commons.misc.lib.function;

import java.util.function.Consumer;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;

@FunctionalInterface
public interface ConsumerThrows<T, E extends Throwable> {
    
    static <T> ConsumerThrows<T, RuntimeException> from(final Consumer<T> c) {
        return c::accept;
    }
    
    static <T> Consumer<T> asConsumer(final ConsumerThrows<T, ?> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (final Throwable e) { // NOSONAR catch throwable to rethrow without declaring
                throw ExceptionUtil.sneakyThrow(e);
            }
        };
    }
    
    void accept(T t) throws E;
    
}
