package com.ixaris.commons.async.lib;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.misc.lib.object.Tuple4;
import com.ixaris.commons.misc.lib.object.Tuple5;
import com.ixaris.commons.misc.lib.object.Wrapper;

/**
 * <ol>
 * <li>Do not use Async types as parameters
 * <li>Only call await() from methods that return Async
 * <li>Be mindful that exceptions are really thrown by the await() call</li>
  * </ol>
 *
 * @param <T>
 */
public interface Async<T> extends CompletionStage<T> {

    Async<Void> COMPLETED = new FutureAsync<>(null);

    /**
     * @return the result holder, transformed to CompletionStage&lt;T&gt;
     */
    static Async<Void> result() {
        return COMPLETED;
    }
    
    /**
     * @param result the asynchronous process result
     * @param <T>
     * @return the result holder, transformed to CompletionStage&lt;T&gt;
     */
    static <T> Async<T> result(final T result) {
        return new FutureAsync<>(result);
    }

    /**
     * Wait for and return the asynchronous process result. Should be used directly on a method call result. Avoid
     * calling with a local variable parameter to avoid misuse since the declared exceptions of the asynchronous method
     * are really thrown by this method.
     * <p>
     * Can only be used in methods that return Async
     *
     * @param async
     * @param <T>
     * @return
     */
    static <T> T await(final Async<T> async) {
        throw new UnsupportedOperationException("Only allowed in methods that return Async<?>. Use AsyncTransformer to transform this code");
    }
    
    /**
     * Similar to await() but returns the Async object instead. Should be used directly on a method call result. Avoid
     * calling with a local variable parameter to avoid misuse since the declared exceptions of the asynchronous method
     * are really thrown by this method.
     * <p>
     * Can only be used in methods that return Async
     *
     * @param async
     * @param <T>
     * @return
     */
    static <T> Async<T> awaitResult(final Async<T> async) {
        throw new UnsupportedOperationException("Only allowed in methods that return Async<?>. Use AsyncTransformer to transform this code");
    }
    
    /**
     * Interop method to convert from Async&lt;T&gt; to CompletionStage&lt;T&gt;
     *
     * @param async the async
     * @return the completion stage
     */
    @Deprecated
    static <T> CompletionStage<T> async(final Async<T> async) {
        return async;
    }

    /**
     * Interop method to convert from Async&lt;T&gt; returning method throwing exceptions to CompletionStage&lt;T&gt;
     *
     * @param async the async
     * @return the completion stage
     */
    static <T> CompletionStage<T> async(final CallableThrows<Async<T>, ?> async) {
        try {
            return async.call();
        } catch (final Throwable t) {
            return CompletionStageUtil.rejected(t);
        }
    }
    
    /**
     * Interop method to convert from CompletionStage&lt;T&gt; to Async&lt;T&gt;
     *
     * @param stage the completion stage
     * @return the async
     */
    static <T> Async<T> async(final CompletionStage<T> stage) {
        if (stage instanceof Async) {
            return (Async<T>) stage;
        } else {
            return new DelegatingAsync<>(stage);
        }
    }
    
    /**
     * Try not to use this method, except maybe in tests!
     * Blocks until the future is resolved
     *
     * @param async the async
     * @return the result
     */
    static <T> T block(final Async<T> async) throws InterruptedException {
        return CompletionStageUtil.block(async);
    }
    
    /**
     * Try not to use this method, except maybe in tests!
     * Blocks until the future is resolved
     *
     * @param async the async
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the result
     */
    static <T> T block(final Async<T> async, final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
        return CompletionStageUtil.block(async, timeout, unit);
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails.
     * This variant is for results of the same type. returns List instead of array due to java's
     * inability to create a generic array. Alternative signature could be
     * <pre>&lt;T&gt; Async&lt;T[]&gt; allSame(Async&lt;T&gt;... asyncs, Class&lt;T&gt; type)</pre>
     *
     * @return a future resolved with a list of results in the same order as the futures argument,
     *         or rejected with the first failure from the given futures
     */
    @SafeVarargs
    static <T> Async<List<T>> allSame(final Async<T>... asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.allSame(asyncs));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for results of the same type
     *
     * @return a future resolved with a list of results in the same order as the futures argument,
     *         or rejected with the first failure from the given futures
     */
    static <T> Async<List<T>> allSame(final List<Async<T>> asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.allSame(asyncs));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails.
     * This variant is for results of the same type
     *
     * @return a future resolved with a map of results corresponding to the keys in the futures argument,
     *         or rejected with the first failure from the given futures
     */
    static <K, V> Async<Map<K, V>> allSame(final Map<K, Async<V>> asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.allSame(asyncs));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for results of any type
     *
     * @return a future resolved with an array of results in the same order or the futures parameter,
     *         or rejected with the first failure from the given futures
     */
    static Async<Object[]> all(final Async<?>... asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.all(asyncs));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for results of any type
     *
     * @return a future resolved with a list of results in the same order as the futures argument,
     *         or rejected with the first failure from the given futures
     */
    static Async<List<Object>> all(final List<Async<?>> asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.all(asyncs));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails.
     * This variant is for results of the same type
     *
     * @return a future resolved with a map of results corresponding to the keys in the futures argument,
     *         or rejected with the first failure from the given futures
     */
    static <K> Async<Map<K, ?>> all(final Map<K, Async<?>> asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.all(asyncs));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 2 results of any type
     */
    static <T1, T2> Async<Tuple2<T1, T2>> all(final Async<T1> p1, final Async<T2> p2) {
        return new DelegatingAsync<>(CompletionStageUtil.all(p1, p2));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 3 results of any type
     */
    static <T1, T2, T3> Async<Tuple3<T1, T2, T3>> all(final Async<T1> p1,
                                                      final Async<T2> p2,
                                                      final Async<T3> p3) {
        return new DelegatingAsync<>(CompletionStageUtil.all(p1, p2, p3));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 4 results of any type
     */
    static <T1, T2, T3, T4> Async<Tuple4<T1, T2, T3, T4>> all(final Async<T1> p1,
                                                              final Async<T2> p2,
                                                              final Async<T3> p3,
                                                              final Async<T4> p4) {
        return new DelegatingAsync<>(CompletionStageUtil.all(p1, p2, p3, p4));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 5 results of any type
     */
    static <T1, T2, T3, T4, T5> Async<Tuple5<T1, T2, T3, T4, T5>> all(final Async<T1> p1,
                                                                      final Async<T2> p2,
                                                                      final Async<T3> p3,
                                                                      final Async<T4> p4,
                                                                      final Async<T5> p5) {
        return new DelegatingAsync<>(CompletionStageUtil.all(p1, p2, p3, p4, p5));
    }
    
    /**
     * use to throw a consistent error indicating that code was not transformed. Typically not used by code unless
     * it is some low level infrastructure code, e.g. proxying calls
     */
    static UnsupportedOperationException noTransformation() {
        return new UnsupportedOperationException("Transform this code using AsyncTransformer");
    }

    /**
     * Synchronously map an async task result to another type. DO NOT BLOCK! To call further asynchronous tasks,
     * use await()
     *
     * @param function
     * @param <U>
     * @param <E>
     * @return
     * @throws E
     */
    default <U, E extends Exception> Async<U> map(final FunctionThrows<T, U, E> function) throws E {
        return new DelegatingAsync<>(CompletionStageUtil.map(this, function));
    }

    final class FutureAsync<T> extends CompletableFuture<T> implements Async<T> {

        FutureAsync() {}

        FutureAsync(final T result) {
            complete(result);
        }

    }

    final class DelegatingAsync<T> implements Async<T>, Wrapper<CompletionStage<T>> {

        private final CompletionStage<T> wrapped;

        DelegatingAsync(final CompletionStage<T> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public CompletionStage<T> unwrap() {
            return wrapped;
        }

        // completion stage wrapper methods

        @Override
        public <U> CompletionStage<U> thenApply(final Function<? super T, ? extends U> fn) {
            return wrapped.thenApply(fn);
        }

        @Override
        public <U> CompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn) {
            return wrapped.thenApplyAsync(fn);
        }

        @Override
        public <U> CompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn, final Executor executor) {
            return wrapped.thenApplyAsync(fn, executor);
        }

        @Override
        public CompletionStage<Void> thenAccept(final Consumer<? super T> action) {
            return wrapped.thenAccept(action);
        }

        @Override
        public CompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action) {
            return wrapped.thenAcceptAsync(action);
        }

        @Override
        public CompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action, final Executor executor) {
            return wrapped.thenAcceptAsync(action, executor);
        }

        @Override
        public CompletionStage<Void> thenRun(final Runnable action) {
            return wrapped.thenRun(action);
        }

        @Override
        public CompletionStage<Void> thenRunAsync(final Runnable action) {
            return wrapped.thenRunAsync(action);
        }

        @Override
        public CompletionStage<Void> thenRunAsync(final Runnable action, final Executor executor) {
            return wrapped.thenRunAsync(action, executor);
        }

        @Override
        public <U, V> CompletionStage<V> thenCombine(final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn) {
            return wrapped.thenCombine(other, fn);
        }

        @Override
        public <U, V> CompletionStage<V> thenCombineAsync(final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn) {
            return wrapped.thenCombineAsync(other, fn);
        }

        @Override
        public <U, V> CompletionStage<V> thenCombineAsync(final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn, final Executor executor) {
            return wrapped.thenCombineAsync(other, fn, executor);
        }

        @Override
        public <U> CompletionStage<Void> thenAcceptBoth(final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action) {
            return wrapped.thenAcceptBoth(other, action);
        }

        @Override
        public <U> CompletionStage<Void> thenAcceptBothAsync(final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action) {
            return wrapped.thenAcceptBothAsync(other, action);
        }

        @Override
        public <U> CompletionStage<Void> thenAcceptBothAsync(final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action, final Executor executor) {
            return wrapped.thenAcceptBothAsync(other, action, executor);
        }

        @Override
        public CompletionStage<Void> runAfterBoth(final CompletionStage<?> other, final Runnable action) {
            return wrapped.runAfterBoth(other, action);
        }

        @Override
        public CompletionStage<Void> runAfterBothAsync(final CompletionStage<?> other, final Runnable action) {
            return wrapped.runAfterBothAsync(other, action);
        }

        @Override
        public CompletionStage<Void> runAfterBothAsync(final CompletionStage<?> other, final Runnable action, final Executor executor) {
            return wrapped.runAfterBothAsync(other, action, executor);
        }

        @Override
        public <U> CompletionStage<U> applyToEither(final CompletionStage<? extends T> other, final Function<? super T, U> fn) {
            return wrapped.applyToEither(other, fn);
        }

        @Override
        public <U> CompletionStage<U> applyToEitherAsync(final CompletionStage<? extends T> other, final Function<? super T, U> fn) {
            return wrapped.applyToEitherAsync(other, fn);
        }

        @Override
        public <U> CompletionStage<U> applyToEitherAsync(final CompletionStage<? extends T> other, final Function<? super T, U> fn, final Executor executor) {
            return wrapped.applyToEitherAsync(other, fn, executor);
        }

        @Override
        public CompletionStage<Void> acceptEither(final CompletionStage<? extends T> other, final Consumer<? super T> action) {
            return wrapped.acceptEither(other, action);
        }

        @Override
        public CompletionStage<Void> acceptEitherAsync(final CompletionStage<? extends T> other, final Consumer<? super T> action) {
            return wrapped.acceptEitherAsync(other, action);
        }

        @Override
        public CompletionStage<Void> acceptEitherAsync(final CompletionStage<? extends T> other, final Consumer<? super T> action, final Executor executor) {
            return wrapped.acceptEitherAsync(other, action, executor);
        }

        @Override
        public CompletionStage<Void> runAfterEither(final CompletionStage<?> other, final Runnable action) {
            return wrapped.runAfterEither(other, action);
        }

        @Override
        public CompletionStage<Void> runAfterEitherAsync(final CompletionStage<?> other, final Runnable action) {
            return wrapped.runAfterEitherAsync(other, action);
        }

        @Override
        public CompletionStage<Void> runAfterEitherAsync(final CompletionStage<?> other, final Runnable action, final Executor executor) {
            return wrapped.runAfterEitherAsync(other, action, executor);
        }

        @Override
        public <U> CompletionStage<U> thenCompose(final Function<? super T, ? extends CompletionStage<U>> fn) {
            return wrapped.thenCompose(fn);
        }

        @Override
        public <U> CompletionStage<U> thenComposeAsync(final Function<? super T, ? extends CompletionStage<U>> fn) {
            return wrapped.thenComposeAsync(fn);
        }

        @Override
        public <U> CompletionStage<U> thenComposeAsync(final Function<? super T, ? extends CompletionStage<U>> fn, final Executor executor) {
            return wrapped.thenComposeAsync(fn, executor);
        }

        @Override
        public CompletionStage<T> exceptionally(final Function<Throwable, ? extends T> fn) {
            return wrapped.exceptionally(fn);
        }

        @Override
        public CompletionStage<T> whenComplete(final BiConsumer<? super T, ? super Throwable> action) {
            return wrapped.whenComplete(action);
        }

        @Override
        public CompletionStage<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> action) {
            return wrapped.whenCompleteAsync(action);
        }

        @Override
        public CompletionStage<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> action, final Executor executor) {
            return wrapped.whenCompleteAsync(action, executor);
        }

        @Override
        public <U> CompletionStage<U> handle(final BiFunction<? super T, Throwable, ? extends U> fn) {
            return wrapped.handle(fn);
        }

        @Override
        public <U> CompletionStage<U> handleAsync(final BiFunction<? super T, Throwable, ? extends U> fn) {
            return wrapped.handleAsync(fn);
        }

        @Override
        public <U> CompletionStage<U> handleAsync(final BiFunction<? super T, Throwable, ? extends U> fn, final Executor executor) {
            return wrapped.handleAsync(fn, executor);
        }

        @Override
        public CompletableFuture<T> toCompletableFuture() {
            return wrapped.toCompletableFuture();
        }

    }

}
