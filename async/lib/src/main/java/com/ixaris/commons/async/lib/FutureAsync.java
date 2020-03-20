package com.ixaris.commons.async.lib;

import java.util.concurrent.CompletableFuture;

public final class FutureAsync<T> extends CompletableFuture<T> implements Async<T> {
    
    public FutureAsync() {}
    
    FutureAsync(final T result) {
        complete(result);
    }
    
}
