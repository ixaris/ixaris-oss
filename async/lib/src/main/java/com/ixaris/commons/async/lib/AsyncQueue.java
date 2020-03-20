package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import com.ixaris.commons.misc.lib.function.RunnableThrows;

/**
 * A queue of {@link Async}s that are chained together on completion. Use to serialize access to a shared resource.
 *
 * <p>Use the static methods exec(long/string, callable) for resources identified by a long/string id.
 */
public final class AsyncQueue {
    
    @FunctionalInterface
    public interface DoneCallback {
        
        /**
         * Called when a promise is done
         */
        void onDone(boolean lastStage);
        
    }
    
    /**
     * @deprecated use {@link LongIdAsyncQueue#exec(String, long, CompletionStageCallableThrows)}
     */
    @Deprecated
    public static <T, E extends Exception> Async<T> exec(final String name,
                                                         final long id,
                                                         final CompletionStageCallableThrows<T, E> task) throws E {
        return LongIdAsyncQueue.exec(name, id, task);
    }
    
    /**
     * @deprecated use {@link StringIdAsyncQueue#exec(String, String, CompletionStageCallableThrows)}
     */
    @Deprecated
    public static <T, E extends Exception> Async<T> exec(final String name,
                                                         final String id,
                                                         final CompletionStageCallableThrows<T, E> task) throws E {
        return StringIdAsyncQueue.exec(name, id, task);
    }
    
    /**
     * @deprecated use {@link StringIdAsyncQueue#exec(String, String, RunnableThrows)}
     */
    @Deprecated
    public static <E extends Exception> Async<Void> exec(final String name,
                                                         final String id,
                                                         final RunnableThrows<E> task) throws E {
        return StringIdAsyncQueue.exec(name, id, task);
    }
    
    private final boolean forkIfFirstInQueue;
    final AtomicReference<FutureAsync<?>> lastStage;
    
    public AsyncQueue() {
        this(false);
    }
    
    public AsyncQueue(final boolean forkIfFirstInQueue) {
        this.forkIfFirstInQueue = forkIfFirstInQueue;
        lastStage = new AtomicReference<>();
    }
    
    public <T, E extends Exception> Async<T> exec(final CompletionStageCallableThrows<T, E> task) {
        final FutureAsync<T> stage = new FutureAsync<>();
        exec(stage, task, null);
        return stage;
    }
    
    public <T, E extends Exception> void exec(final FutureAsync<T> stage,
                                              final CompletionStageCallableThrows<T, E> task,
                                              final DoneCallback doneCallback) {
        internalExec(stage, task, doneCallback);
    }
    
    public <E extends Exception> Async<Void> exec(final RunnableThrows<E> task) {
        final FutureAsync<Void> stage = new FutureAsync<>();
        exec(stage, task, null);
        return stage;
    }
    
    public <E extends Exception> void exec(final FutureAsync<Void> stage,
                                           final RunnableThrows<E> task,
                                           final DoneCallback doneCallback) {
        internalExec(stage,
            () -> {
                task.run();
                return result();
            },
            doneCallback);
    }
    
    private <T, E extends Exception> void internalExec(final FutureAsync<T> stage,
                                                       final CompletionStageCallableThrows<T, E> task,
                                                       final DoneCallback doneCallback) {
        final FutureAsync<?> prevStage = lastStage.getAndSet(stage);
        if (forkIfFirstInQueue || (prevStage != null)) {
            // preserve current thread's async local and trace
            final Runnable runnable = AsyncLocal.wrap(AsyncTrace.wrap(() -> CompletableFutureUtil.complete(stage, task)));
            
            final Executor executor = AsyncExecutor.get();
            if (prevStage == null) {
                executor.execute(runnable);
            } else {
                CompletionStageUtil.whenDone(prevStage, (r, t) -> executor.execute(runnable));
            }
        } else {
            CompletableFutureUtil.complete(stage, task);
        }
        
        CompletionStageUtil.whenDone(stage, (r, t) -> {
            final boolean isLastStage = lastStage.compareAndSet(stage, null);
            if (doneCallback != null) {
                doneCallback.onDone(isLastStage);
            }
        });
    }
    
}
