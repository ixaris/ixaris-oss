package com.ixaris.commons.async.lib.executor;

import java.util.concurrent.Executor;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.misc.lib.object.Wrapper;

/**
 * Wrapper for an {@link Executor} to wrap jobs using {@link AsyncExecutor}
 */
public class AsyncExecutorWrapper<E extends Executor> implements Executor, Wrapper<E> {
    
    private final boolean allowJoin;
    protected final E wrapped;
    
    public AsyncExecutorWrapper(final boolean allowJoin, final E wrapped) {
        if (wrapped == null) {
            throw new IllegalArgumentException("wrapped is null");
        }
        
        this.allowJoin = allowJoin;
        this.wrapped = wrapped;
    }
    
    public AsyncExecutorWrapper(final E wrapped) {
        this(true, wrapped);
    }
    
    public boolean isAllowJoin() {
        return allowJoin;
    }
    
    @Override
    public final void execute(final Runnable command) {
        wrapped.execute(AsyncExecutor.wrap(this, command));
    }
    
    @Override
    public final E unwrap() {
        return wrapped;
    }
    
}
