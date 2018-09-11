package com.ixaris.commons.async.lib;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import com.ixaris.commons.misc.lib.function.CallableThrows;

@FunctionalInterface
public interface CompletionStageCallableThrows<T, E extends Throwable> {
    
    @SuppressWarnings("unchecked")
    static <T> CompletionStageCallableThrows<T, Exception> from(final Callable<? extends CompletionStage<T>> c) {
        return c::call;
    }
    
    @SuppressWarnings("unchecked")
    static <T, E extends Throwable> CompletionStageCallableThrows<T, E> from(final CallableThrows<? extends CompletionStage<T>, E> c) {
        return c::call;
    }
    
    CompletionStage<T> call() throws E;
    
}
