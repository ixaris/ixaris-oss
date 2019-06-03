package com.ixaris.commons.async.lib.executor;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.misc.lib.object.Wrapper;
import java.util.concurrent.Executor;

/**
 * Wrapper for an {@link Executor} to wrap jobs using {@link AsyncExecutor}
 */
public class AsyncExecutorWrapper<E extends Executor> implements Executor, Wrapper<E> {
    
    protected final E wrapped;
    
    public AsyncExecutorWrapper(final E wrapped) {
        if (wrapped == null) {
            throw new IllegalArgumentException("wrapped is null");
        }
        
        this.wrapped = wrapped;
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
