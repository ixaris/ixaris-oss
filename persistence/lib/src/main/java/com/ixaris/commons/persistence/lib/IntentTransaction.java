package com.ixaris.commons.persistence.lib;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.persistence.lib.exception.DuplicateIntentException;

@FunctionalInterface
public interface IntentTransaction<T, E extends Exception> {
    
    default T throwOnDuplicateIntent() throws DuplicateIntentException {
        return onDuplicateIntent(e -> {
            throw ExceptionUtil.sneakyThrow(e);
        });
    }
    
    T onDuplicateIntent(FunctionThrows<DuplicateIntentException, T, E> idempotencyFunction);
    
}
