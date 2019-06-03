package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.from;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.ixaris.commons.async.lib.CommonsAsyncLib.AsyncLocalValue;
import com.ixaris.commons.async.lib.CommonsAsyncLib.AsyncLocals;
import com.ixaris.commons.collections.lib.GuavaCollections;
import com.ixaris.commons.misc.lib.function.BiFunctionThrows;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.ConsumerThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Works like thread local but across an async process. Uses thread locals internally.
 *
 * <p>Async local values are expected to be immutable, since async locals can be used concurrently across threads if the
 * process forks
 */
@SuppressWarnings("squid:ClassCyclomaticComplexity")
public class AsyncLocal<T> {
    
    @FunctionalInterface
    public interface AsyncLocalValidatorTransformer<T> {
        
        /**
         * Validates the value against the previous value (stack head) and throw an exception if invalid (or always
         * throw if the current value is not null for non-stackable).
         *
         * <p>The function may also transform the value, e.g. make it immutable or concatenate with the previous value.
         *
         * <p>It is suggested that the validation be re-entrant, i.e. allow the same value to be set (the implementation
         * supports this and unsets only when the first block finishes). This can be achieved by checking that the value
         * is equal to the previous value, considering any transformations that may be applied.
         */
        T validateAndTransform(T value, T prevValue);
        
    }
    
    public static final class Snapshot {
        
        private final ImmutableMap<AsyncLocal<?>, Object> map;
        
        private Snapshot() {
            map = ASYNC_LOCALS.get(); // return same instance (immutable)
        }
        
        private Snapshot(final AsyncLocals locals) {
            final ImmutableMap.Builder<AsyncLocal<?>, Object> builder = ImmutableMap.builder();
            locals
                .getAsyncLocalMap()
                .forEach((k, v) -> {
                    final AsyncLocal<?> asyncLocal = ASYNC_LOCAL_MAP.get(k);
                    if (asyncLocal == null) {
                        throw new IllegalStateException(String.format("AsyncLocal for %s not found", k));
                    }
                    try {
                        builder.put(asyncLocal, asyncLocal.decode(v));
                    } catch (final InvalidProtocolBufferException e) {
                        throw new IllegalStateException(e);
                    }
                });
            map = builder.build();
        }
        
        public Map<AsyncLocal<?>, Object> getMap() {
            return map;
        }
        
        public AsyncLocals encode() {
            final AsyncLocals.Builder builder = AsyncLocals.newBuilder();
            map.forEach((k, v) -> {
                final AsyncLocalValue encoded = encode(k, (v instanceof Stack) ? ((Stack) v).peek() : v);
                if (encoded != null) {
                    builder.putAsyncLocal(k.key, encoded);
                }
            });
            return builder.build();
        }
        
        @SuppressWarnings("unchecked")
        private static <T> AsyncLocalValue encode(final AsyncLocal<T> asyncLocal, final Object value) {
            return asyncLocal.encode((T) value);
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
        
        public <V, E extends Exception> Async<V> exec(final CompletionStageCallableThrows<V, E> task) throws E {
            if (task == null) {
                throw new IllegalArgumentException("task is null");
            }
            
            final ImmutableMap<AsyncLocal<?>, Object> prevAsyncLocals = ASYNC_LOCALS.get();
            ImmutableMap<AsyncLocal<?>, Object> asyncLocals = prevAsyncLocals;
            for (final Entry<AsyncLocal<?>, Object> entry : map.entrySet()) {
                asyncLocals = entry.getKey().applyValue(entry.getValue(), asyncLocals);
            }
            return executeAndRestoreAsyncLocals(task, asyncLocals, prevAsyncLocals);
        }
        
        public <V, E extends Exception> V exec(final CallableThrows<V, E> task) throws E {
            if (task == null) {
                throw new IllegalArgumentException("task is null");
            }
            
            final ImmutableMap<AsyncLocal<?>, Object> prevAsyncLocals = ASYNC_LOCALS.get();
            ImmutableMap<AsyncLocal<?>, Object> asyncLocals = prevAsyncLocals;
            for (final Entry<AsyncLocal<?>, Object> entry : map.entrySet()) {
                asyncLocals = entry.getKey().applyValue(entry.getValue(), asyncLocals);
            }
            return executeAndRestoreAsyncLocals(task, asyncLocals, prevAsyncLocals);
        }
        
        public <E extends Exception> void exec(final RunnableThrows<E> task) throws E {
            if (task == null) {
                throw new IllegalArgumentException("task is null");
            }
            
            final ImmutableMap<AsyncLocal<?>, Object> prevAsyncLocals = ASYNC_LOCALS.get();
            ImmutableMap<AsyncLocal<?>, Object> asyncLocals = prevAsyncLocals;
            for (final Entry<AsyncLocal<?>, Object> entry : map.entrySet()) {
                asyncLocals = entry.getKey().applyValue(entry.getValue(), asyncLocals);
            }
            executeAndRestoreAsyncLocals(task, asyncLocals, prevAsyncLocals);
        }
        
    }
    
    // Instances should not be changed once placed in async local map
    static final class Stack<E> {
        
        private final Object[] array;
        
        static <E> Stack<E> init(final E first, final E second) {
            final Object[] newStack = new Object[2];
            newStack[0] = second;
            newStack[1] = first;
            return new Stack<>(newStack);
        }
        
        static <E> Stack<E> copyAndPush(final Stack<E> stack, final E next) {
            final Object[] newStack = new Object[stack.array.length + 1];
            newStack[0] = next;
            System.arraycopy(stack.array, 0, newStack, 1, stack.array.length);
            return new Stack<>(newStack);
        }
        
        private Stack(final Object[] stack) {
            this.array = stack;
        }
        
        @SuppressWarnings("unchecked")
        public E peek() {
            return (E) array[0];
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, o, other -> Arrays.equals(array, other.array));
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(array);
        }
        
    }
    
    private static volatile ImmutableMap<String, AsyncLocal<?>> ASYNC_LOCAL_MAP = ImmutableMap.of();
    
    public static <T> Callable<T> wrap(final Callable<T> callable) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return () -> executeAndRestoreAsyncLocals(CallableThrows.from(callable), snapshot, ASYNC_LOCALS.get());
    }
    
    public static Runnable wrap(final Runnable runnable) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return () -> executeAndRestoreAsyncLocals(RunnableThrows.from(runnable), snapshot, ASYNC_LOCALS.get());
    }
    
    public static <T> Consumer<T> wrap(final Consumer<T> consumer) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return v -> executeAndRestoreAsyncLocals(() -> consumer.accept(v), snapshot, ASYNC_LOCALS.get());
    }
    
    public static <T, R> Function<T, R> wrap(final Function<T, R> function) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return t -> executeAndRestoreAsyncLocals(() -> function.apply(t), snapshot, ASYNC_LOCALS.get());
    }
    
    public static <T, U> BiConsumer<T, U> wrap(final BiConsumer<T, U> biConsumer) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return (t, u) -> executeAndRestoreAsyncLocals(() -> biConsumer.accept(t, u), snapshot, ASYNC_LOCALS.get());
    }
    
    public static <T, U, R> BiFunction<T, U, R> wrapThrows(final BiFunction<T, U, R> biFunction) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return (t, u) -> executeAndRestoreAsyncLocals(() -> biFunction.apply(t, u), snapshot, ASYNC_LOCALS.get());
    }
    
    public static <V, E extends Exception> CallableThrows<V, E> wrapThrows(final CallableThrows<V, E> callable) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return () -> executeAndRestoreAsyncLocals(callable, snapshot, ASYNC_LOCALS.get());
    }
    
    public static <E extends Exception> RunnableThrows<E> wrapThrows(final RunnableThrows<E> runnable) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return () -> executeAndRestoreAsyncLocals(runnable, snapshot, ASYNC_LOCALS.get());
    }
    
    public static <T, E extends Exception> ConsumerThrows<T, E> wrapThrows(final ConsumerThrows<T, E> consumer) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return t -> executeAndRestoreAsyncLocals(() -> consumer.accept(t), snapshot, ASYNC_LOCALS.get());
    }
    
    public static <T, R, E extends Exception> FunctionThrows<T, R, E> wrapThrows(
        final FunctionThrows<T, R, E> function
    ) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return t -> executeAndRestoreAsyncLocals(() -> function.apply(t), snapshot, ASYNC_LOCALS.get());
    }
    
    public static <T, U, R, E extends Exception> BiFunctionThrows<T, U, R, E> wrapThrows(
        final BiFunctionThrows<T, U, R, E> biFunction
    ) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return (t, u) -> executeAndRestoreAsyncLocals(() -> biFunction.apply(t, u), snapshot, ASYNC_LOCALS.get());
    }
    
    public static <T, E extends Exception> CompletionStageCallableThrows<T, E> wrapThrows(
        final CompletionStageCallableThrows<T, E> callable
    ) {
        final ImmutableMap<AsyncLocal<?>, Object> snapshot = ASYNC_LOCALS.get();
        return () -> executeAndRestoreAsyncLocals(callable, snapshot, ASYNC_LOCALS.get());
    }
    
    public static <T> Builder with(final AsyncLocal<T> asyncLocal, final T value) {
        return new Builder().with(asyncLocal, value);
    }
    
    public static Snapshot snapshot() {
        return new Snapshot();
    }
    
    public static Snapshot snapshot(final AsyncLocals locals) {
        return new Snapshot(locals);
    }
    
    public static <V, E extends Exception> Async<V> exec(
        final Snapshot snapshot, final CompletionStageCallableThrows<V, E> callable
    ) throws E {
        if (callable == null) {
            throw new IllegalArgumentException("callable is null");
        }
        
        return executeAndRestoreAsyncLocals(callable, snapshot.map, ASYNC_LOCALS.get());
    }
    
    public static <V, E extends Exception> V exec(
        final Snapshot snapshot, final CallableThrows<V, E> callable
    ) throws E {
        if (callable == null) {
            throw new IllegalArgumentException("callable is null");
        }
        
        return executeAndRestoreAsyncLocals(callable, snapshot.map, ASYNC_LOCALS.get());
    }
    
    public static <E extends Exception> void exec(final Snapshot snapshot, final RunnableThrows<E> runnable) throws E {
        if (runnable == null) {
            throw new IllegalArgumentException("runnable is null");
        }
        
        executeAndRestoreAsyncLocals(runnable, snapshot.map, ASYNC_LOCALS.get());
    }
    
    @SuppressWarnings("squid:S1698")
    private static <V, E extends Exception> Async<V> executeAndRestoreAsyncLocals(
        final CompletionStageCallableThrows<V, E> callable,
        final ImmutableMap<AsyncLocal<?>, Object> asyncLocals,
        final ImmutableMap<AsyncLocal<?>, Object> prevAsyncLocals
    ) throws E {
        if (asyncLocals == prevAsyncLocals) {
            return from(callable.call());
        } else {
            ASYNC_LOCALS.set(asyncLocals);
            try {
                return from(callable.call());
            } finally {
                // restore on current thread
                ASYNC_LOCALS.set(prevAsyncLocals);
            }
        }
    }
    
    @SuppressWarnings("squid:S1698")
    private static <V, E extends Exception> V executeAndRestoreAsyncLocals(
        final CallableThrows<V, E> callable,
        final ImmutableMap<AsyncLocal<?>, Object> asyncLocals,
        final ImmutableMap<AsyncLocal<?>, Object> prevAsyncLocals
    ) throws E {
        if (asyncLocals == prevAsyncLocals) {
            return callable.call();
        } else {
            ASYNC_LOCALS.set(asyncLocals);
            try {
                return callable.call();
            } finally {
                ASYNC_LOCALS.set(prevAsyncLocals);
            }
        }
    }
    
    @SuppressWarnings("squid:S1698")
    private static <E extends Exception> void executeAndRestoreAsyncLocals(
        final RunnableThrows<E> runnable,
        final ImmutableMap<AsyncLocal<?>, Object> asyncLocals,
        final ImmutableMap<AsyncLocal<?>, Object> prevAsyncLocals
    ) throws E {
        if (asyncLocals == prevAsyncLocals) {
            runnable.run();
        } else {
            ASYNC_LOCALS.set(asyncLocals);
            try {
                runnable.run();
            } finally {
                ASYNC_LOCALS.set(prevAsyncLocals);
            }
        }
    }
    
    private static final ThreadLocal<ImmutableMap<AsyncLocal<?>, Object>> ASYNC_LOCALS = ThreadLocal.withInitial(
        ImmutableMap::of
    );
    
    private final String key;
    private final AsyncLocalValidatorTransformer<T> validatorTransformer;
    
    public AsyncLocal(final String key) {
        this(key, false);
    }
    
    /**
     * @param stackable true for a stackable value, false otherwise
     */
    @SuppressWarnings("squid:S864")
    public AsyncLocal(final String key, final boolean stackable) {
        this(
            key,
            stackable
                ? (v, p) -> v
                : (v, p) -> {
                    if ((p != null) && !p.equals(v)) {
                        throw new IllegalStateException(String.format("%s already has associated value [%s]", key, p));
                    }
                    return v;
                }
        );
    }
    
    /**
     * @param validatorTransformer the value validator.
     */
    public AsyncLocal(final String key, final AsyncLocalValidatorTransformer<T> validatorTransformer) {
        if (key == null) {
            throw new IllegalArgumentException("key should not be null");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException(String.format("Key [%s] too short. Min length allowed is 1", key));
        }
        if (key.length() > 50) {
            throw new IllegalArgumentException(String.format("Key [%s] too long. Max length allowed is %d", key, 50));
        }
        if (validatorTransformer == null) {
            throw new IllegalArgumentException("validatorTransformer is null");
        }
        
        this.key = key;
        this.validatorTransformer = validatorTransformer;
        
        synchronized (AsyncLocal.class) {
            if (ASYNC_LOCAL_MAP.containsKey(key)) {
                throw new IllegalStateException(String.format("Already registered AsyncLocal with key %s", key));
            }
            ASYNC_LOCAL_MAP = GuavaCollections.copyOfMapAdding(ASYNC_LOCAL_MAP, key, this);
        }
    }
    
    /**
     * encode async local value. default implementation returns null to avoid encoding
     *
     * @return null if this should not be encoded
     * @throws UnsupportedOperationException if impossible to encoded, e.g. database transaction
     */
    @SuppressWarnings("squid:S1172")
    public AsyncLocalValue encode(final T value) {
        return null;
    }
    
    /**
     * decode async local value
     *
     * @throws UnsupportedOperationException if should not be decoded (see {@link #encode(Object)})
     */
    public T decode(final AsyncLocalValue value) throws InvalidProtocolBufferException {
        throw new UnsupportedOperationException();
    }
    
    @SuppressWarnings("unchecked")
    public final T get() {
        Object o = ASYNC_LOCALS.get().get(this);
        if (o instanceof Stack) {
            o = ((Stack) o).peek();
        }
        return (T) o;
    }
    
    public final <V, E extends Exception> Async<V> exec(
        final T value, final CompletionStageCallableThrows<V, E> task
    ) throws E {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        
        final ImmutableMap<AsyncLocal<?>, Object> prevAsyncLocals = ASYNC_LOCALS.get();
        final ImmutableMap<AsyncLocal<?>, Object> asyncLocals = applyValue(value, prevAsyncLocals);
        return executeAndRestoreAsyncLocals(task, asyncLocals, prevAsyncLocals);
    }
    
    public final <V, E extends Exception> V exec(final T value, final CallableThrows<V, E> task) throws E {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        
        final ImmutableMap<AsyncLocal<?>, Object> prevAsyncLocals = ASYNC_LOCALS.get();
        final ImmutableMap<AsyncLocal<?>, Object> asyncLocals = applyValue(value, prevAsyncLocals);
        return executeAndRestoreAsyncLocals(task, asyncLocals, prevAsyncLocals);
    }
    
    public final <E extends Exception> void exec(final T value, final RunnableThrows<E> task) throws E {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        
        final ImmutableMap<AsyncLocal<?>, Object> prevAsyncLocals = ASYNC_LOCALS.get();
        final ImmutableMap<AsyncLocal<?>, Object> asyncLocals = applyValue(value, prevAsyncLocals);
        executeAndRestoreAsyncLocals(task, asyncLocals, prevAsyncLocals);
    }
    
    @Override
    public boolean equals(final Object o) {
        // equality is based on key (these are typically singletons, but could be instances of a class)
        return EqualsUtil.equals(this, o, other -> key.equals(other.key));
    }
    
    @Override
    public int hashCode() {
        return key.hashCode();
    }
    
    @Override
    public String toString() {
        return key;
    }
    
    /**
     * @return the async local map of values, which is the same instance if there is no change
     */
    @SuppressWarnings("unchecked")
    private ImmutableMap<AsyncLocal<?>, Object> applyValue(
        final Object value, final ImmutableMap<AsyncLocal<?>, Object> asyncLocals
    ) {
        final Object currentObject = asyncLocals.get(this);
        if (currentObject == null) {
            // for non-stackable values, single value is placed without wrapping in a stack
            return GuavaCollections.copyOfMapAdding(asyncLocals, this, value);
            
        } else if (!(currentObject instanceof Stack)) {
            final T current = (T) currentObject;
            final T transformed = validatorTransformer.validateAndTransform((T) value, current);
            if (current.equals(transformed)) {
                // same value, so just run
                return asyncLocals;
            } else {
                // second value causes stack to be created
                return GuavaCollections.copyOfMapAdding(asyncLocals, this, Stack.init(current, transformed));
            }
            
        } else {
            final Stack<T> stack = (Stack<T>) currentObject;
            final T current = stack.peek(); // current can never be null
            final T transformed = validatorTransformer.validateAndTransform((T) value, current);
            if (current.equals(transformed)) {
                // same value, so just run
                return asyncLocals;
            } else {
                return GuavaCollections.copyOfMapAdding(asyncLocals, this, Stack.copyAndPush(stack, transformed));
            }
        }
    }
    
}
