package com.ixaris.commons.async.lib.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

/**
 * Supplies an api for setting thread locals and unsetting at the end of the task.
 */
public class ThreadLocalHelper {
    
    public static final class Builder {
        
        private final Map<ThreadLocal<?>, Object> map = new HashMap<>();
        
        private Builder() {}
        
        public <T> Builder with(final ThreadLocal<T> threadLocal, final T value) {
            if (threadLocal == null) {
                throw new IllegalArgumentException("threadLocal is null");
            }
            
            map.put(threadLocal, value);
            return this;
        }
        
        public <V, E extends Throwable> V exec(final CallableThrows<V, E> task) throws E {
            if (task == null) {
                throw new IllegalArgumentException("task is null");
            }
            
            return executeAndRestoreThreadLocals(task, map);
        }
        
        public <E extends Throwable> void exec(final RunnableThrows<E> task) throws E {
            exec(() -> {
                task.run();
                return null;
            });
        }
        
    }
    
    public static <T> Builder with(final ThreadLocal<T> threadLocal, final T value) {
        return new Builder().with(threadLocal, value);
    }
    
    public static <T, V, E extends Throwable> V exec(final ThreadLocal<T> threadLocal, final T value, final CallableThrows<V, E> task) throws E {
        if (threadLocal == null) {
            throw new IllegalArgumentException("threadLocal is null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        
        if (threadLocal.get() != null) {
            throw new IllegalStateException(threadLocal + " already set to [" + threadLocal.get() + "] while setting to [" + value + "]");
        }
        threadLocal.set(value);
        try {
            return task.call();
        } finally {
            threadLocal.remove();
        }
    }
    
    public static <T, E extends Throwable> void exec(final ThreadLocal<T> threadLocal, final T value, final RunnableThrows<E> task) throws E {
        exec(threadLocal, value, () -> {
            task.run();
            return null;
        });
    }
    
    private static <V, E extends Throwable> V executeAndRestoreThreadLocals(final CallableThrows<V, E> callable,
                                                                            final Map<ThreadLocal<?>, Object> threadLocals) throws E {
        try {
            for (final Entry<ThreadLocal<?>, Object> entry : threadLocals.entrySet()) {
                final ThreadLocal<?> threadLocal = entry.getKey();
                if (threadLocal.get() != null) {
                    throw new IllegalStateException(threadLocal + " already set to [" + threadLocal.get() + "]");
                }
                setValue(threadLocal, entry.getValue());
            }
            return callable.call();
        } finally {
            for (final ThreadLocal<?> threadLocal : threadLocals.keySet()) {
                threadLocal.remove();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> void setValue(final ThreadLocal<T> threadLocal, final Object value) {
        threadLocal.set((T) value);
    }
    
    private ThreadLocalHelper() {}
    
}
