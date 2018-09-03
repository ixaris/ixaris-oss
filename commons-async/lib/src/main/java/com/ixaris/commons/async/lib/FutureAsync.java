package com.ixaris.commons.async.lib;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class FutureAsync<T> extends CompletableFuture<T> implements Async<T> {
    
    public static <T> FutureAsync<T> fromCompletionStage(final CompletionStage<T> stage) {
        final FutureAsync<T> future = new FutureAsync<>();
        CompletableFutureUtil.complete(future, stage);
        return future;
    }
    
    public FutureAsync() {}
    
    FutureAsync(final T result) {
        complete(result);
    }
    
}
