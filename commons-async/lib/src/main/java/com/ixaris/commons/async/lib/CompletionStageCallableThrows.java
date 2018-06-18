package com.ixaris.commons.async.lib;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import com.ixaris.commons.misc.lib.function.CallableThrows;

public interface CompletionStageCallableThrows<T, E extends Throwable> extends CallableThrows<CompletionStage<T>, E> {
    
    @SuppressWarnings("unchecked")
    static <T> CompletionStageCallableThrows<T, Exception> from(final Callable<CompletionStage<T>> c) {
        return c::call;
    }
    
}
