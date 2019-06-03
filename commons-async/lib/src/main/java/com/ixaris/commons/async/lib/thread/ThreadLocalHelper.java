package com.ixaris.commons.async.lib.thread;

import static com.ixaris.commons.async.lib.Async.from;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageCallableThrows;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Supplies an api for setting thread locals and unsetting at the end of the task.
 */
public class ThreadLocalHelper {
    
    public static final class Builder {
        
        private static Map<ThreadLocal<?>, Object> setAndExtractPrev(Map<ThreadLocal<?>, Object> map) {
            final Map<ThreadLocal<?>, Object> prev = new HashMap<>();
            for (final Entry<ThreadLocal<?>, Object> entry : map.entrySet()) {
                final ThreadLocal<?> threadLocal = entry.getKey();
                prev.put(threadLocal, threadLocal.get());
                setValue(threadLocal, entry.getValue());
            }
            return prev;
        }
        
        private static void restorePrev(Map<ThreadLocal<?>, Object> prev) {
            for (final Entry<ThreadLocal<?>, Object> entry : prev.entrySet()) {
                setValue(entry.getKey(), entry.getValue());
            }
        }
        
        @SuppressWarnings("unchecked")
        private static <T> void setValue(final ThreadLocal<T> threadLocal, final Object value) {
            threadLocal.set((T) value);
        }
        
        private final Map<ThreadLocal<?>, Object> map = new HashMap<>();
        
        private Builder() {}
        
        public <T> Builder with(final ThreadLocal<T> threadLocal, final T value) {
            if (threadLocal == null) {
                throw new IllegalArgumentException("threadLocal is null");
            }
            
            map.put(threadLocal, value);
            return this;
        }
        
        public <V, E extends Exception> V exec(final CallableThrows<V, E> task) throws E {
            if (task == null) {
                throw new IllegalArgumentException("task is null");
            }
            
            final Map<ThreadLocal<?>, Object> prev = setAndExtractPrev(map);
            try {
                return task.call();
            } finally {
                restorePrev(prev);
            }
        }
        
        public <E extends Exception> void exec(final RunnableThrows<E> task) throws E {
            if (task == null) {
                throw new IllegalArgumentException("task is null");
            }
            
            final Map<ThreadLocal<?>, Object> prev = setAndExtractPrev(map);
            try {
                task.run();
            } finally {
                restorePrev(prev);
            }
        }
        
    }
    
    public static <T> Builder with(final ThreadLocal<T> threadLocal, final T value) {
        return new Builder().with(threadLocal, value);
    }
    
    public static <T, E extends Exception> void exec(
        final ThreadLocal<T> threadLocal, final T value, final RunnableThrows<E> task
    ) throws E {
        if (threadLocal == null) {
            throw new IllegalArgumentException("threadLocal is null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        
        final T prev = threadLocal.get();
        threadLocal.set(value);
        try {
            task.run();
        } finally {
            threadLocal.set(prev);
        }
    }
    
    public static <T, V, E extends Exception> V exec(
        final ThreadLocal<T> threadLocal, final T value, final CallableThrows<V, E> task
    ) throws E {
        if (threadLocal == null) {
            throw new IllegalArgumentException("threadLocal is null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        
        final T prev = threadLocal.get();
        threadLocal.set(value);
        try {
            return task.call();
        } finally {
            threadLocal.set(prev);
        }
    }
    
    public static <T, V, E extends Exception> Async<V> exec(
        final ThreadLocal<T> threadLocal, final T value, final CompletionStageCallableThrows<V, E> task
    ) throws E {
        if (threadLocal == null) {
            throw new IllegalArgumentException("threadLocal is null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        
        final T prev = threadLocal.get();
        threadLocal.set(value);
        try {
            return from(task.call());
        } finally {
            threadLocal.set(prev);
        }
    }
    
    private ThreadLocalHelper() {}
    
}
