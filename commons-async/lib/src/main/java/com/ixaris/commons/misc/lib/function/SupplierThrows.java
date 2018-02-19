package com.ixaris.commons.misc.lib.function;

import java.util.function.Supplier;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;

@FunctionalInterface
public interface SupplierThrows<T, E extends Throwable> {
    
    static <T> SupplierThrows<T, RuntimeException> from(final Supplier<T> s) {
        return s::get;
    }
    
    static <T> Supplier<T> asSupplier(final SupplierThrows<T, ?> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (final Throwable e) { // NOSONAR catch throwable to rethrow without declaring
                throw ExceptionUtil.sneakyThrow(e);
            }
        };
    }
    
    T get() throws E;
    
}
