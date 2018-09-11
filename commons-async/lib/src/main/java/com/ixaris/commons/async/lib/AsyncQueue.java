package com.ixaris.commons.async.lib;

import com.ixaris.commons.misc.lib.function.RunnableThrows;

/**
 * A queue of {@link Async}s that are chained together on completion. Use to serialize access to a shared resource.
 * <p>
 * Use the static methods exec(long/string, callable) for resources identified by a long/string id.
 */
public final class AsyncQueue {
    
    public static <T, E extends Exception> Async<T> exec(final String name,
                                                         final String id,
                                                         final CompletionStageCallableThrows<T, E> task) throws E {
        final FutureAsync<T> async = new FutureAsync<>();
        CompletionStageQueue.exec(name, id, async, task);
        return async;
    }
    
    public static <E extends Exception> Async<Void> exec(final String name,
                                                         final String id,
                                                         final RunnableThrows<E> task) throws E {
        final FutureAsync<Void> async = new FutureAsync<>();
        CompletionStageQueue.exec(name, id, async, task);
        return async;
    }
    
    public static <T, E extends Exception> Async<T> exec(final String name,
                                                         final long id,
                                                         final CompletionStageCallableThrows<T, E> task) throws E {
        final FutureAsync<T> async = new FutureAsync<>();
        CompletionStageQueue.exec(name, id, async, task);
        return async;
    }
    
    public static <E extends Exception> Async<Void> exec(final String name,
                                                         final long id,
                                                         final RunnableThrows<E> task) throws E {
        final FutureAsync<Void> async = new FutureAsync<>();
        CompletionStageQueue.exec(name, id, async, task);
        return async;
    }
    
    private final CompletionStageQueue queue;
    
    public AsyncQueue() {
        this(false);
    }
    
    public AsyncQueue(final boolean forkIfFirstInQueue) {
        queue = new CompletionStageQueue(forkIfFirstInQueue);
    }
    
    public <T, E extends Exception> Async<T> exec(final CompletionStageCallableThrows<T, E> task) throws E {
        final FutureAsync<T> async = new FutureAsync<>();
        queue.exec(async, task);
        return async;
    }
    
    public <E extends Exception> Async<Void> exec(final RunnableThrows<E> task) throws E {
        final FutureAsync<Void> async = new FutureAsync<>();
        queue.exec(async, task);
        return async;
    }
    
}
