package com.ixaris.commons.async.lib;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import com.ixaris.commons.collections.lib.AbstractLazyReadWriteLockedLongMap;
import com.ixaris.commons.collections.lib.AbstractLazyReadWriteLockedMap;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * A queue of {@link CompletionStage}s that are chained together on completion. Use to serialize access to a shared resource.
 * <p>
 * Use the static methods exec(long/string, callable) for resources identified by a long/string id.
 */
public final class CompletionStageQueue {
    
    @FunctionalInterface
    private interface DoneCallback {
        
        /**
         * Called when a promise is done
         */
        void onDone();
        
    }
    
    public static final class StringMap extends AbstractLazyReadWriteLockedMap<String, CompletionStageQueue, CompletableFuture<?>, CompletableFuture<?>> {
        
        /**
         * Queue execution of the given callable. The callable is executed once all previous promises are completed. Use this when
         * the callable returns a result that is converted to a promise by the queue
         *
         * @param id the shared resource id
         * @param callable callable that returns a result, to be converted to a promise
         * @param <T> the type returned by the callable and the type of the promise
         * @return a promise with result of the same type returned by the callable
         */
        public <T, E extends Exception> CompletionStage<T> exec(final String id, final CallableThrows<CompletionStage<T>, E> callable) {
            final CompletableFuture<T> stage = new CompletableFuture<>();
            final Tuple2<CompletionStageQueue, CompletableFuture<?>> queue = getOrCreate(id, stage);
            return queue.get1().internalExec(queue.get2(), stage, callable, () -> {
                if (queue.get1().lastStage.get() == null) {
                    tryRemove(id);
                }
            });
        }
        
        public <E extends Exception> CompletionStage<Void> exec(final String id, final RunnableThrows<E> runnable) {
            return exec(id, () -> {
                runnable.run();
                return CompletableFuture.completedFuture(null);
            });
        }
        
        @Override
        protected Tuple2<CompletionStageQueue, CompletableFuture<?>> create(final CompletableFuture<?> stage) {
            return tuple(new CompletionStageQueue(stage), null);
        }
        
        @Override
        protected CompletableFuture<?> existing(final CompletionStageQueue value, final CompletableFuture<?> stage) {
            return value.lastStage.getAndSet(stage);
        }
        
        @Override
        protected boolean shouldRemove(final CompletionStageQueue queue) {
            return queue.lastStage.get() == null;
        }
        
    }
    
    public static final class LongMap extends AbstractLazyReadWriteLockedLongMap<CompletionStageQueue, CompletableFuture<?>, CompletableFuture<?>> {
        
        /**
         * Queue execution of the given callable. The callable is executed once all previous promises are completed. Use this when
         * the callable returns a result that is converted to a promise by the queue
         *
         * @param id the shared resource id
         * @param callable callable that returns a result, to be converted to a promise
         * @param <T> the type returned by the callable and the type of the promise
         * @return a promise with result of the same type returned by the callable
         */
        public <T, E extends Exception> CompletionStage<T> exec(final long id, final CallableThrows<CompletionStage<T>, E> callable) {
            final CompletableFuture<T> stage = new CompletableFuture<>();
            final Tuple2<CompletionStageQueue, CompletableFuture<?>> queue = getOrCreate(id, stage);
            return queue.get1().internalExec(queue.get2(), stage, callable, () -> {
                if (queue.get1().lastStage.get() == null) {
                    tryRemove(id);
                }
            });
        }
        
        public <E extends Exception> CompletionStage<Void> exec(final long id, final RunnableThrows<E> runnable) {
            return exec(id, () -> {
                runnable.run();
                return CompletableFuture.completedFuture(null);
            });
        }
        
        @Override
        protected Tuple2<CompletionStageQueue, CompletableFuture<?>> create(final CompletableFuture<?> stage) {
            return tuple(new CompletionStageQueue(stage), null);
        }
        
        @Override
        protected CompletableFuture<?> existing(final CompletionStageQueue value, final CompletableFuture<?> stage) {
            return value.lastStage.getAndSet(stage);
        }
        
        @Override
        protected boolean shouldRemove(final CompletionStageQueue queue) {
            return queue.lastStage.get() == null;
        }
        
    }
    
    private static final AbstractLazyReadWriteLockedMap<String, StringMap, Void, Void> STRING_QUEUES =
        new AbstractLazyReadWriteLockedMap<String, StringMap, Void, Void>() {
            
            @Override
            protected Tuple2<StringMap, Void> create(final Void v) {
                return tuple(new StringMap(), null);
            }
            
        };
    
    private static final AbstractLazyReadWriteLockedMap<String, LongMap, Void, Void> LONG_QUEUES =
        new AbstractLazyReadWriteLockedMap<String, LongMap, Void, Void>() {
            
            @Override
            protected Tuple2<LongMap, Void> create(final Void v) {
                return tuple(new LongMap(), null);
            }
            
        };
    
    public static <T, E extends Exception> CompletionStage<T> exec(final String name,
                                                                   final String id,
                                                                   final CallableThrows<CompletionStage<T>, E> callable) {
        return STRING_QUEUES.getOrCreate(name, null).get1().exec(id, callable);
    }
    
    public static <E extends Exception> CompletionStage<Void> exec(final String name,
                                                                   final String id,
                                                                   final RunnableThrows<E> runnable) {
        return STRING_QUEUES.getOrCreate(name, null).get1().exec(id, runnable);
    }
    
    public static <T, E extends Exception> CompletionStage<T> exec(final String name,
                                                                   final long id,
                                                                   final CallableThrows<CompletionStage<T>, E> callable) {
        return LONG_QUEUES.getOrCreate(name, null).get1().exec(id, callable);
    }
    
    public static <E extends Exception> CompletionStage<Void> exec(final String name,
                                                                   final long id,
                                                                   final RunnableThrows<E> runnable) {
        return LONG_QUEUES.getOrCreate(name, null).get1().exec(id, runnable);
    }
    
    private final AtomicReference<CompletableFuture<?>> lastStage;
    
    /**
     * Default constructor with an empty queue
     */
    public CompletionStageQueue() {
        lastStage = new AtomicReference<>();
    }
    
    /**
     * Constructor with an initial promise
     */
    public CompletionStageQueue(final CompletableFuture<?> initialValue) {
        lastStage = new AtomicReference<>(initialValue);
    }
    
    public <T, E extends Exception> CompletionStage<T> exec(final CallableThrows<CompletionStage<T>, E> execCallable) {
        final CompletableFuture<T> stage = new CompletableFuture<>();
        final CompletableFuture<?> prevStage = lastStage.getAndSet(stage);
        return internalExec(prevStage, stage, execCallable, null);
    }
    
    private <T, E extends Exception> CompletableFuture<T> internalExec(final CompletableFuture<?> prevStage,
                                                                       final CompletableFuture<T> stage,
                                                                       final CallableThrows<CompletionStage<T>, E> execCallable,
                                                                       final DoneCallback doneCallback) {
        // preserve current thread's async local
        final CallableThrows<CompletionStage<T>, E> wrappedCallable = AsyncLocal.wrapThrows(execCallable);
        final Executor executor = AsyncExecutor.get();
        if (prevStage == null) {
            executor.execute(AsyncTrace.wrap(() -> {
                try {
                    wrappedCallable.call().whenComplete((rr, tt) -> {
                        if (tt == null) {
                            stage.complete(rr);
                        } else {
                            stage.completeExceptionally(tt);
                        }
                    });
                } catch (Throwable tt) { // NOSONAR future handling
                    stage.completeExceptionally(AsyncTrace.join(tt));
                }
            }));
        } else {
            prevStage.whenComplete((r, t) -> executor.execute(AsyncTrace.wrap(() -> {
                try {
                    wrappedCallable.call().whenComplete((rr, tt) -> {
                        if (tt == null) {
                            stage.complete(rr);
                        } else {
                            stage.completeExceptionally(tt);
                        }
                    });
                } catch (Throwable tt) { // NOSONAR future handling
                    stage.completeExceptionally(AsyncTrace.join(tt));
                }
            })));
        }
        
        stage.whenComplete((r, t) -> {
            lastStage.compareAndSet(stage, null);
            if (doneCallback != null) {
                doneCallback.onDone();
            }
        });
        return stage;
    }
    
}
