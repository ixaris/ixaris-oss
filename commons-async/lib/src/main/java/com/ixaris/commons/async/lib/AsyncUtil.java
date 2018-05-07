package com.ixaris.commons.async.lib;

import com.ixaris.commons.async.lib.Async.DelegatingAsync;
import com.ixaris.commons.async.lib.Async.FutureAsync;
import com.ixaris.commons.misc.lib.function.FunctionThrows;

public class AsyncUtil {
    
    public static <T> Async<T> rejected(final Throwable t) {
        final FutureAsync<T> f = new FutureAsync<>();
        CompletableFutureUtil.reject(f, t);
        return f;
    }
    
    public static boolean isDone(final Async<?> async) {
        return CompletionStageUtil.isDone(async);
    }
    
    public static <T> T get(final Async<T> async) {
        return CompletionStageUtil.get(async);
    }
    
    public static <T, U> Async<U> doneCompose(final Async<T> async, final FunctionThrows<Async<T>, Async<U>, ?> function) {
        return new DelegatingAsync<>(CompletionStageUtil.doneCompose(async, function));
    }
    
    private AsyncUtil() {}
    
}
