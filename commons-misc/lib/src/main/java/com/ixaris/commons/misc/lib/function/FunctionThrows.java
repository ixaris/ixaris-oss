package com.ixaris.commons.misc.lib.function;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;
import java.util.function.Function;

@FunctionalInterface
public interface FunctionThrows<T, R, E extends Throwable> {
    
    static <T> FunctionThrows<T, T, RuntimeException> identity() {
        return t -> t;
    }
    
    static <T, R> FunctionThrows<T, R, RuntimeException> from(final Function<T, R> f) {
        return f::apply;
    }
    
    static <T, R> Function<T, R> asFunction(final FunctionThrows<T, R, ?> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (final Throwable e) { // NOSONAR catch throwable to rethrow without declaring
                throw ExceptionUtil.sneakyThrow(e);
            }
        };
    }
    
    R apply(T t) throws E;
    
}
