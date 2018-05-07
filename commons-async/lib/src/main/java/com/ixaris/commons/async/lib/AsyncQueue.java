package com.ixaris.commons.async.lib;

import com.ixaris.commons.async.lib.Async.FutureAsync;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

/**
 * A queue of {@link Async}s that are chained together on completion. Use to serialize access to a shared resource.
 * <p>
 * Use the static methods exec(long/string, callable) for resources identified by a long/string id.
 */
public final class AsyncQueue {

    public static <T, E extends Exception> Async<T> exec(final String name,
                                                         final String id,
                                                         final CallableThrows<Async<T>, E> callable) throws E {
        final FutureAsync<T> async = new FutureAsync<>();
        CompletionStageQueue.exec(name, id, async, callable);
        return async;
    }

    public static <E extends Exception> Async<Void> exec(final String name,
                                                         final String id,
                                                         final RunnableThrows<E> runnable) throws E {
        final FutureAsync<Void> async = new FutureAsync<>();
        CompletionStageQueue.exec(name, id, async, runnable);
        return async;
    }

    public static <T, E extends Exception> Async<T> exec(final String name,
                                                         final long id,
                                                         final CallableThrows<Async<T>, E> callable) throws E {
        final FutureAsync<T> async = new FutureAsync<>();
        CompletionStageQueue.exec(name, id, async, callable);
        return async;
    }

    public static <E extends Exception> Async<Void> exec(final String name,
                                                         final long id,
                                                         final RunnableThrows<E> runnable) throws E {
        final FutureAsync<Void> async = new FutureAsync<>();
        CompletionStageQueue.exec(name, id, async, runnable);
        return async;
    }

    private final CompletionStageQueue queue;

    public AsyncQueue() {
        queue = new CompletionStageQueue();
    }

    public <T, E extends Exception> Async<T> exec(final CallableThrows<Async<T>, E> execCallable) throws E {
        final FutureAsync<T> async = new FutureAsync<>();
        queue.exec(async, execCallable);
        return async;
    }

}
