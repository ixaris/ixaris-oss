package com.ixaris.commons.async.lib;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ixaris.commons.misc.lib.function.BiFunctionThrows;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.ConsumerThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

/**
 * Works like thread local but across an async process. Uses thread locals internally.
 *
 * Local value can be stackable. If stackable, value will be pushed to a stack and popped afterwards, otherwise an error
 * is thrown if the local value is already set.
 *
 * Map or async locals and stacks of values are expected to be immutable. do not change once placed in thread local as these can then
 * be copied to other threads
 */
public final class AsyncLocal<T> {
    
    @FunctionalInterface
    public interface AsyncLocalValidator<T> {
        
        void validate(T value, T prevValue);
        
    }
    
    public static final class Snapshot {
        
        private final Map<AsyncLocal<?>, Object> map;
        
        private Snapshot() {
            map = ASYNC_LOCALS.get(); // return same instance (immutable)
        }
        
        public Map<AsyncLocal<?>, Object> getMap() {
            return Collections.unmodifiableMap(map);
        }
    }
    
    public static final class Builder {
        
        private final Map<AsyncLocal<?>, Object> map = new HashMap<>();
        
        private Builder() {}
        
        public <T> Builder with(final AsyncLocal<T> asyncLocal, final T value) {
            if (asyncLocal == null) {
                throw new IllegalArgumentException("asyncLocal is null");
            }
            if (value == null) {
                throw new IllegalArgumentException("value is null");
            }
            
            map.put(asyncLocal, value);
            return this;
        }
        
        public <V, E extends Throwable> V exec(final CallableThrows<V, E> task) throws E {
            if (task == null) {
                throw new IllegalArgumentException("task is null");
            }
            
            final Map<AsyncLocal<?>, Object> asyncLocals = ASYNC_LOCALS.get();
            Map<AsyncLocal<?>, Object> newAsyncLocals = asyncLocals;
            for (final Entry<AsyncLocal<?>, Object> entry : map.entrySet()) {
                newAsyncLocals = applyValue(newAsyncLocals, entry.getKey(), entry.getValue());
            }
            return executeAndRestoreAsyncLocals(task, asyncLocals, newAsyncLocals);
        }
        
        public <E extends Throwable> void exec(final RunnableThrows<E> task) throws E {
            exec(() -> {
                task.run();
                return null;
            });
        }
        
        @SuppressWarnings("unchecked")
        private static <T> Map<AsyncLocal<?>, Object> applyValue(final Map<AsyncLocal<?>, Object> newAsyncLocals,
                                                                 final AsyncLocal<T> asyncLocal,
                                                                 final Object value) {
            return asyncLocal.applyValue((T) value, newAsyncLocals);
        }
        
    }
    
    // Instances should not be changed once placed in async local map
    private static final class Stack<E> extends LinkedList<E> {
        
        private static final long serialVersionUID = 1L;
        
        static <E> Stack<E> copyAndPush(final Stack<E> stack, final E next) {
            final Stack<E> newStack = new Stack<>(stack);
            newStack.push(next);
            return newStack;
        }
        
        private Stack(final E first, final E second) {
            super();
            push(first);
            push(second);
        }
        
        private Stack(final Collection<? extends E> c) {
            super(c);
        }
    }
    
    public static <T> Callable<T> wrap(final Callable<T> callable) {
        final Snapshot snapshot = AsyncLocal.snapshot();
        return () -> exec(snapshot, CallableThrows.from(callable));
    }
    
    public static Runnable wrap(final Runnable runnable) {
        final Snapshot snapshot = AsyncLocal.snapshot();
        return () -> exec(snapshot, RunnableThrows.from(runnable));
    }
    
    public static <T> Consumer<T> wrap(final Consumer<T> consumer) {
        final Snapshot snapshot = AsyncLocal.snapshot();
        return v -> exec(snapshot, () -> consumer.accept(v));
    }
    
    public static <I, V> Function<I, V> wrap(final Function<I, V> function) {
        final Snapshot snapshot = AsyncLocal.snapshot();
        return i -> exec(snapshot, () -> function.apply(i));
    }
    
    public static <I1, I2, V> BiFunction<I1, I2, V> wrapThrows(final BiFunction<I1, I2, V> biFunction) {
        final Snapshot snapshot = AsyncLocal.snapshot();
        return (i1, i2) -> exec(snapshot, () -> biFunction.apply(i1, i2));
    }
    
    public static <V, E extends Throwable> CallableThrows<V, E> wrapThrows(final CallableThrows<V, E> callable) {
        final Snapshot snapshot = AsyncLocal.snapshot();
        return () -> exec(snapshot, callable);
    }
    
    public static <E extends Throwable> RunnableThrows<E> wrapThrows(final RunnableThrows<E> runnable) {
        final Snapshot snapshot = AsyncLocal.snapshot();
        return () -> exec(snapshot, runnable);
    }
    
    public static <V, E extends Throwable> ConsumerThrows<V, E> wrapThrows(final ConsumerThrows<V, E> consumer) {
        final Snapshot snapshot = AsyncLocal.snapshot();
        return v -> exec(snapshot, () -> consumer.accept(v));
    }
    
    public static <I, V, E extends Throwable> FunctionThrows<I, V, E> wrapThrows(final FunctionThrows<I, V, E> function) {
        final Snapshot snapshot = AsyncLocal.snapshot();
        return i -> exec(snapshot, () -> function.apply(i));
    }
    
    public static <I1, I2, V, E extends Throwable> BiFunctionThrows<I1, I2, V, E> wrapThrows(final BiFunctionThrows<I1, I2, V, E> biFunction) {
        final Snapshot snapshot = AsyncLocal.snapshot();
        return (i1, i2) -> exec(snapshot, () -> biFunction.apply(i1, i2));
    }
    
    public static <T> Builder with(final AsyncLocal<T> asyncLocal, final T value) {
        return new Builder().with(asyncLocal, value);
    }
    
    public static Snapshot snapshot() {
        return new Snapshot();
    }
    
    public static <V, E extends Throwable> V exec(final Snapshot snapshot, final CallableThrows<V, E> callable) throws E {
        if (callable == null) {
            throw new IllegalArgumentException("callable is null");
        }
        
        return executeAndRestoreAsyncLocals(callable, ASYNC_LOCALS.get(), snapshot.map);
    }
    
    public static <E extends Throwable> void exec(final Snapshot snapshot, final RunnableThrows<E> runnable) throws E {
        if (runnable == null) {
            throw new IllegalArgumentException("runnable is null");
        }
        
        executeAndRestoreAsyncLocals(runnable, ASYNC_LOCALS.get(), snapshot.map);
    }
    
    private static <V, E extends Throwable> V executeAndRestoreAsyncLocals(final CallableThrows<V, E> callable,
                                                                           final Map<AsyncLocal<?>, Object> setAsyncLocals,
                                                                           final Map<AsyncLocal<?>, Object> newAsyncLocals) throws E {
        if (setAsyncLocals == newAsyncLocals) { // NOSONAR explicitly checking reference as these are immutable
            return callable.call();
        } else {
            ASYNC_LOCALS.set(newAsyncLocals);
            try {
                return callable.call();
            } finally {
                ASYNC_LOCALS.set(setAsyncLocals);
            }
        }
    }
    
    private static <E extends Throwable> void executeAndRestoreAsyncLocals(final RunnableThrows<E> runnable,
                                                                           final Map<AsyncLocal<?>, Object> setAsyncLocals,
                                                                           final Map<AsyncLocal<?>, Object> newAsyncLocals) throws E {
        if (setAsyncLocals == newAsyncLocals) { // NOSONAR explicitly checking reference as these are immutable
            runnable.run();
        } else {
            ASYNC_LOCALS.set(newAsyncLocals);
            try {
                runnable.run();
            } finally {
                ASYNC_LOCALS.set(setAsyncLocals);
            }
        }
    }
    
    // this map is immutable DO NOT CHANGE, swap the value instead
    private static final ThreadLocal<Map<AsyncLocal<?>, Object>> ASYNC_LOCALS = ThreadLocal.withInitial(Collections::emptyMap);
    
    private final AsyncLocalValidator<T> validator;
    private final boolean stackable;
    
    public AsyncLocal() {
        this(false);
    }
    
    /**
     * @param stackable true for a stackable value, false otherwise
     */
    public AsyncLocal(final boolean stackable) {
        this.stackable = stackable;
        this.validator = null;
    }
    
    /**
     * Create a stackable asynclocal. This constructor takes a function that validates the value against the current stack and may throw an exception if invalid.
     * The validator is only called when there is a value already set, otherwise it is assumed that the value can be set. For validating the initial value, this
     * should be done by the entry point
     *
     * @param validator the value validator.
     */
    public AsyncLocal(final AsyncLocalValidator<T> validator) {
        if (validator == null) {
            throw new IllegalArgumentException("validator is null");
        }
        
        this.stackable = true;
        this.validator = validator;
    }
    
    @SuppressWarnings("unchecked")
    public T get() {
        Object o = ASYNC_LOCALS.get().get(this);
        if (stackable && (o instanceof Stack)) {
            o = ((Stack) o).peek();
        }
        return (T) o;
    }
    
    public <V, E extends Throwable> V exec(final T value, final CallableThrows<V, E> task) throws E {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        
        final Map<AsyncLocal<?>, Object> asyncLocals = ASYNC_LOCALS.get();
        final Map<AsyncLocal<?>, Object> newAsyncLocals = applyValue(value, asyncLocals);
        return executeAndRestoreAsyncLocals(task, asyncLocals, newAsyncLocals);
    }
    
    public <E extends Throwable> void exec(final T value, final RunnableThrows<E> task) throws E {
        exec(value, () -> {
            task.run();
            return null;
        });
    }
    
    /**
     * @return the async local map of values, which is the same instance if there is no change
     */
    @SuppressWarnings("unchecked")
    private <E extends Throwable> Map<AsyncLocal<?>, Object> applyValue(final T value,
                                                                        final Map<AsyncLocal<?>, Object> asyncLocals) throws E {
        final Object currentObject = asyncLocals.get(this);
        if (currentObject == null) {
            // for non-stackable values, single value is placed without wrapping in a stack
            return copyAndPut(asyncLocals, this, value);
            
        } else if (!stackable) {
            if (currentObject.equals(value)) {
                // same value, so just run
                return asyncLocals;
            } else {
                throw new IllegalStateException("Already associated value [" + currentObject + "]");
            }
            
        } else if (!(currentObject instanceof Stack)) {
            if (currentObject.equals(value)) {
                // same value, so just run
                return asyncLocals;
            } else {
                final T current = (T) currentObject;
                if (validator != null) {
                    validator.validate(value, current);
                }
                // second value causes stack to be created
                return copyAndPut(asyncLocals, this, new Stack<>(current, value));
            }
            
        } else {
            final Stack<T> stack = (Stack<T>) currentObject;
            final T current = stack.peek();
            if (current.equals(value)) {
                // same value, so just run
                return asyncLocals;
            } else {
                if (validator != null) {
                    validator.validate(value, current);
                }
                return copyAndPut(asyncLocals, this, Stack.copyAndPush(stack, value));
            }
        }
    }
    
    private Map<AsyncLocal<?>, Object> copyAndPut(final Map<AsyncLocal<?>, Object> asyncLocal, final AsyncLocal<T> key, final Object value) {
        final Map<AsyncLocal<?>, Object> copy = new HashMap<>(asyncLocal); // NOSONAR map is immutable once returned
        copy.put(key, value);
        return copy;
    }
    
}
