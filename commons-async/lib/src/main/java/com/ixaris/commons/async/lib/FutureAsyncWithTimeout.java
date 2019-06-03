package com.ixaris.commons.async.lib;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

public final class FutureAsyncWithTimeout<T> extends CompletableFuture<T> implements Async<T> {
    
    private volatile ScheduledFuture<?> scheduledFuture;
    
    public FutureAsyncWithTimeout() {}
    
    FutureAsyncWithTimeout(final T result) {
        complete(result);
    }
    
    void setScheduledFutureAsync(final ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }
    
    @Override
    public boolean complete(final T value) {
        if (super.complete(value)) {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public boolean completeExceptionally(final Throwable t) {
        if (super.completeExceptionally(t)) {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            return true;
        } else {
            return false;
        }
    }
    
}
