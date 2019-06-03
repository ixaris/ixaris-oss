package com.ixaris.commons.misc.lib.function;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;
import java.util.function.BiFunction;

@FunctionalInterface
public interface BiFunctionThrows<T, U, R, E extends Throwable> {
    
    static <T, U, R> BiFunctionThrows<T, U, R, RuntimeException> from(final BiFunction<T, U, R> f) {
        return f::apply;
    }
    
    static <T, U, R> BiFunction<T, U, R> asFunction(final BiFunctionThrows<T, U, R, ?> function) {
        return (t, u) -> {
            try {
                return function.apply(t, u);
            } catch (final Throwable e) { // NOSONAR catch throwable to rethrow without declaring
                throw ExceptionUtil.sneakyThrow(e);
            }
        };
    }
    
    R apply(T t, U u) throws E;
    
}
