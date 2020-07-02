package com.ixaris.commons.persistence.lib;

import static com.ixaris.commons.misc.lib.exception.ExceptionUtil.sneakyThrow;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.persistence.lib.exception.DuplicateIntentException;

@FunctionalInterface
public interface AsyncIntentTransaction<T, E extends Exception> {
    
    @SuppressWarnings("squid:S1160")
    default Async<T> throwOnDuplicateIntent() throws E, DuplicateIntentException {
        return onDuplicateIntent(e -> {
            throw sneakyThrow(e);
        });
    }
    
    Async<T> onDuplicateIntent(FunctionThrows<DuplicateIntentException, T, E> idempotencyFunction) throws E;
    
}
