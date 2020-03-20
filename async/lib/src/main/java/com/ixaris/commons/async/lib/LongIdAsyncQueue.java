package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.collections.lib.AbstractLazyLockedMap;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

public class LongIdAsyncQueue {
    
    private static final AbstractLazyLockedMap<String, LongIdAsyncQueue, Void> NAMED_QUEUES = new AbstractLazyLockedMap<String, LongIdAsyncQueue, Void>() {
        
        @Override
        protected LongIdAsyncQueue create(final Void v) {
            return new LongIdAsyncQueue();
        }
        
    };
    
    public static <T, E extends Exception> Async<T> exec(final String name,
                                                         final long id,
                                                         final CompletionStageCallableThrows<T, E> task) throws E {
        return NAMED_QUEUES.getOrCreate(name, null).exec(id, task);
    }
    
    public static <E extends Exception> Async<Void> exec(final String name,
                                                         final long id,
                                                         final RunnableThrows<E> task) throws E {
        return NAMED_QUEUES.getOrCreate(name, null).exec(id, task);
    }
    
    private static final class LongMap extends AbstractLazyLockedMap<Long, AsyncQueue, Void> {
        
        /**
         * Queue execution of the given callable. The callable is executed once all previous tasks are completed.
         *
         * @param id the shared resource id
         * @param callable callable that returns a result
         * @param <T> the type returned by the callable and the type of the promise
         */
        private <T, E extends Exception> Async<T> exec(final long id, final CompletionStageCallableThrows<T, E> callable) throws E {
            final AsyncQueue queue = getOrCreate(id, null);
            final FutureAsync<T> future = new FutureAsync<>();
            queue.exec(future, callable, lastStage -> {
                // cleanup
                if (lastStage) {
                    tryRemove(id);
                }
            });
            return future;
        }
        
        /**
         * Queue execution of the given callable. The callable is executed once all previous tasks are completed.
         *
         * @param id the shared resource id
         * @param runnable runnable task
         */
        private <E extends Exception> Async<Void> exec(final long id, final RunnableThrows<E> runnable) throws E {
            return exec(id, () -> {
                runnable.run();
                return result();
            });
        }
        
        @Override
        protected AsyncQueue create(final Void v) {
            return new AsyncQueue();
        }
        
        @Override
        protected boolean shouldRemove(final AsyncQueue queue) {
            return queue.lastStage.get() == null;
        }
        
    }
    
    private final LongMap map = new LongMap();
    
    public <T, E extends Exception> Async<T> exec(final long id, final CompletionStageCallableThrows<T, E> task) throws E {
        return map.exec(id, task);
    }
    
    public <E extends Exception> Async<Void> exec(final long id, final RunnableThrows<E> task) throws E {
        return map.exec(id, task);
    }
    
}
