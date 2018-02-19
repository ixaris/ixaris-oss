package com.ixaris.commons.async.lib.scheduler;

import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.scheduler.Scheduler;
import com.ixaris.commons.misc.lib.object.Wrapper;

/**
 * Wrapper for an {@link Scheduler} to wrap jobs using {@link AsyncExecutor}
 */
public class AsyncSchedulerWrapper<S extends Scheduler> implements Scheduler, Wrapper<S> {
    
    protected final S wrapped;
    protected final Executor executor;
    
    public AsyncSchedulerWrapper(final S wrapped, final Executor executor) {
        if (wrapped == null) {
            throw new IllegalArgumentException("wrapped is null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor is null");
        }
        
        this.wrapped = wrapped;
        this.executor = executor;
    }
    
    @Override
    public TimerTask schedule(final Runnable runnable, final long delay, final TimeUnit unit) {
        return wrapped.schedule(AsyncExecutor.wrap(executor, runnable), delay, unit);
    }
    
    @Override
    public TimerTask schedule(final Runnable runnable, final Date time) {
        return wrapped.schedule(AsyncExecutor.wrap(executor, runnable), time);
    }
    
    @Override
    public TimerTask scheduleAtFixedRate(final Runnable runnable, final long initialDelay, final long period, final TimeUnit unit) {
        return wrapped.scheduleAtFixedRate(AsyncExecutor.wrap(executor, runnable), initialDelay, period, unit);
    }
    
    @Override
    public TimerTask scheduleWithFixedDelay(final Runnable runnable, final long initialDelay, final long period, final TimeUnit unit) {
        return wrapped.scheduleWithFixedDelay(AsyncExecutor.wrap(executor, runnable), initialDelay, period, unit);
    }
    
    @Override
    public void shutdown() {
        wrapped.shutdown();
    }
    
    @Override
    public S unwrap() {
        return wrapped;
    }
    
}
