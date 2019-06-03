package com.ixaris.commons.misc.lib.function;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;
import java.util.concurrent.Callable;

@FunctionalInterface
public interface CallableThrows<T, E extends Throwable> {
    
    @SuppressWarnings("unchecked")
    static <T> CallableThrows<T, Exception> from(final Callable<T> c) {
        return c::call;
    }
    
    static <T> Callable<T> asCallable(final CallableThrows<T, ?> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (final Throwable e) { // NOSONAR catch throwable to rethrow without declaring
                throw ExceptionUtil.sneakyThrow(e);
            }
        };
    }
    
    T call() throws E;
    
}
