package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.*;

import com.ixaris.commons.async.lib.CompletionStageQueue.DoneCallback;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

public final class AsyncQueue {
    
    public static <T, E extends Exception> Async<T> exec(final String name,
                                                         final String id,
                                                         final CallableThrows<Async<T>, E> callable) throws E {
        return async(CompletionStageQueue.exec(name, id, () -> async(callable.call())));
    }
    
    public static <E extends Exception> Async<Void> exec(final String name,
                                                         final String id,
                                                         final RunnableThrows<E> runnable) throws E {
        return async(CompletionStageQueue.exec(name, id, runnable));
    }
    
    public static <T, E extends Exception> Async<T> exec(final String name,
                                                         final long id,
                                                         final CallableThrows<Async<T>, E> callable) throws E {
        return async(CompletionStageQueue.exec(name, id, () -> async(callable.call())));
    }
    
    public static <E extends Exception> Async<Void> exec(final String name,
                                                         final long id,
                                                         final RunnableThrows<E> runnable) throws E {
        return async(CompletionStageQueue.exec(name, id, runnable));
    }
    
    private final CompletionStageQueue queue;
    
    public AsyncQueue() {
        queue = new CompletionStageQueue();
    }
    
    public <T, E extends Exception> Async<T> exec(final CallableThrows<Async<T>, E> execCallable,
                                                  final DoneCallback doneCallback) throws E {
        return async(queue.exec(() -> async(execCallable.call()), doneCallback));
    }
    
}
