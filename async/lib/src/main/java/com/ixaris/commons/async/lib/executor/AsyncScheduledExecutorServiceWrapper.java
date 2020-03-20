package com.ixaris.commons.async.lib.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ixaris.commons.async.lib.AsyncExecutor;

/**
 * Wrapper for an {@link ScheduledExecutorService} to wrap jobs using {@link AsyncExecutor}
 */
public class AsyncScheduledExecutorServiceWrapper<E extends ScheduledExecutorService> extends AsyncExecutorServiceWrapper<E> implements ScheduledExecutorService {
    
    public AsyncScheduledExecutorServiceWrapper(final boolean allowJoin, final E wrapped) {
        super(allowJoin, wrapped);
    }
    
    public AsyncScheduledExecutorServiceWrapper(final E wrapped) {
        this(true, wrapped);
    }
    
    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return wrapped.schedule(AsyncExecutor.wrap(this, command), delay, unit);
    }
    
    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return wrapped.schedule(AsyncExecutor.wrap(this, callable), delay, unit);
    }
    
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
        return wrapped.scheduleAtFixedRate(AsyncExecutor.wrap(this, command), initialDelay, period, unit);
    }
    
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        return wrapped.scheduleWithFixedDelay(AsyncExecutor.wrap(this, command), initialDelay, delay, unit);
    }
    
}
