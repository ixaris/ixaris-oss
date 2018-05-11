package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.CompletionStageUtil.convert;

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
     * Async that is rejected with the given throwable. Should typically not be used. Instead, throw exceptions
     * from within async methods
     *
     * @param t
     * @param <T>
     * @return
     */
    static <T> Async<T> rejected(final Throwable t) {
        final FutureAsync<T> f = new FutureAsync<>();
        CompletableFutureUtil.reject(f, t);
        return f;
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
    static <T> T await(final CompletionStage<T> async) {
        throw new UnsupportedOperationException("Only allowed in methods that return Async<?>. Use AsyncTransformer to transform this code");
    }
    
    /**
     * Similar to await() but returns the Async object instead, intended for returning the same result in case of
     * success while recovering from thrown exceptions. Should be used directly on a method call result. Avoid
     * calling with a local variable parameter to avoid misuse since the declared exceptions of the asynchronous method
     * are really thrown by this method.
     * <p>
     * Can only be used in methods that return Async
     *
     * @param async
     * @param <T>
     * @return
     */
    static <T, U extends CompletionStage<T>> U awaitExceptions(final U async) {
        throw new UnsupportedOperationException("Only allowed in methods that return Async<?>. Use AsyncTransformer to transform this code");
    }
    
    /**
     * use awaitExceptions instead
     */
    @Deprecated
    static <T, U extends CompletionStage<T>> U awaitResult(final U async) {
        throw new UnsupportedOperationException("Only allowed in methods that return Async<?>. Use AsyncTransformer to transform this code");
    }
    
    /**
     * No longer needed as {@link Async} extends {@link CompletionStage}
     */
    @Deprecated
    static <T> CompletionStage<T> async(final Async<T> async) {
        return async;
    }
    
    /**
     * Use {@link #from(CompletionStage)}
     */
    @Deprecated
    static <T> Async<T> async(final CompletionStage<T> stage) {
        return from(stage);
    }
    
    /**
     * Interop method to ignore exceptions in the signature since these will not really be thrown but cause the
     * completion stage to be rejected
     *
     * @param async the async
     * @return the completion stage
     */
    static <T> Async<T> from(final CallableThrows<? extends CompletionStage<T>, ?> async) {
        try {
            return from(async.call());
        } catch (final Throwable t) { // NOSONAR framework code needs to catch everything
            // transformed methods should not throw exceptions
            return rejected(t);
        }
    }
    
    /**
     * Interop method to convert from CompletionStage&lt;T&gt; to Async&lt;T&gt;
     *
     * @param stage the completion stage
     * @return the async
     */
    static <T> Async<T> from(final CompletionStage<T> stage) {
        if (stage instanceof Async) {
            return (Async<T>) stage;
        } else {
            return new DelegatingAsync<>(stage);
        }
    }
    
    /**
     * Use CompletionStageUtil.block(async)
     */
    @Deprecated
    static <T> T block(final CompletionStage<T> async) throws InterruptedException {
        return CompletionStageUtil.block(async);
    }
    
    /**
     * Use CompletionStageUtil.block(async)
     */
    @Deprecated
    static <T> T block(final CompletionStage<T> async, final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
        return CompletionStageUtil.block(async, timeout, unit);
    }
    
    /**
     * @see CompletionStageUtil#allSame(CompletionStage[])
     */
    @SafeVarargs
    static <T> Async<List<T>> allSame(final CompletionStage<T>... asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.allSame(asyncs));
    }
    
    /**
     * @see CompletionStageUtil#allSame(List)
     */
    static <T> Async<List<T>> allSame(final List<? extends CompletionStage<T>> asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.allSame(asyncs));
    }
    
    /**
     * @see CompletionStageUtil#allSame(Map)
     */
    static <K, V> Async<Map<K, V>> allSame(final Map<K, ? extends CompletionStage<V>> asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.allSame(asyncs));
    }
    
    /**
     * @see CompletionStageUtil#all(CompletionStage[])
     */
    static Async<Object[]> all(final CompletionStage<?>... asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.all(asyncs));
    }
    
    /**
     * @see CompletionStageUtil#all(List)
     */
    static Async<List<Object>> all(final List<? extends CompletionStage<?>> asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.all(asyncs));
    }
    
    /**
     * @see CompletionStageUtil#all(Map)
     */
    static <K> Async<Map<K, ?>> all(final Map<K, ? extends CompletionStage<?>> asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.all(asyncs));
    }
    
    /**
     * @see CompletionStageUtil#all(CompletionStage, CompletionStage)
     */
    static <T1, T2> Async<Tuple2<T1, T2>> all(final CompletionStage<T1> p1,
                                              final CompletionStage<T2> p2) {
        return new DelegatingAsync<>(CompletionStageUtil.all(p1, p2));
    }
    
    /**
     * @see CompletionStageUtil#all(CompletionStage, CompletionStage, CompletionStage)
     */
    static <T1, T2, T3> Async<Tuple3<T1, T2, T3>> all(final CompletionStage<T1> p1,
                                                      final CompletionStage<T2> p2,
                                                      final CompletionStage<T3> p3) {
        return new DelegatingAsync<>(CompletionStageUtil.all(p1, p2, p3));
    }
    
    /**
     * @see CompletionStageUtil#all(CompletionStage, CompletionStage, CompletionStage, CompletionStage)
     */
    static <T1, T2, T3, T4> Async<Tuple4<T1, T2, T3, T4>> all(final CompletionStage<T1> p1,
                                                              final CompletionStage<T2> p2,
                                                              final CompletionStage<T3> p3,
                                                              final CompletionStage<T4> p4) {
        return new DelegatingAsync<>(CompletionStageUtil.all(p1, p2, p3, p4));
    }
    
    /**
     * @see CompletionStageUtil#all(CompletionStage, CompletionStage, CompletionStage, CompletionStage, CompletionStage)
     */
    static <T1, T2, T3, T4, T5> Async<Tuple5<T1, T2, T3, T4, T5>> all(final CompletionStage<T1> p1,
                                                                      final CompletionStage<T2> p2,
                                                                      final CompletionStage<T3> p3,
                                                                      final CompletionStage<T4> p4,
                                                                      final CompletionStage<T5> p5) {
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
     * Acts like {@link CompletionStage#thenApply(Function)} but allows function to throw any exception, and joins
     * the trace in case of failure
     *
     * @param function the mapping function
     * @param <U> the mapped type
     * @param <E> the type of the failure exception
     * @return the mapped stage
     * @throws E
     */
    default <U, E extends Exception> Async<U> map(final FunctionThrows<T, U, E> function) throws E {
        return new DelegatingAsync<>(thenApply(convert(function)));
    }
    
    final class FutureAsync<T> extends CompletableFuture<T> implements Async<T> {
        
        public static <T> FutureAsync<T> fromCompletionStage(final CompletionStage<T> stage) {
            final FutureAsync<T> future = new FutureAsync<>();
            CompletableFutureUtil.completeFrom(future, stage);
            return future;
        }
        
        public FutureAsync() {}
        
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
