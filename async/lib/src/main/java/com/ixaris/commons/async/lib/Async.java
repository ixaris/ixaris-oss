package com.ixaris.commons.async.lib;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ixaris.commons.async.lib.CompletionStageUtil.CreateArray;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.misc.lib.object.Tuple4;
import com.ixaris.commons.misc.lib.object.Tuple5;

/**
 * <ol>
 *   <li>Only call await() from methods that return Async
 *   <li>Be mindful that exceptions are really thrown by the await() call
 * </ol>
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
     * @return the result holder, transformed to CompletionStage&lt;T&gt;
     */
    static <T> Async<T> result(final T result) {
        return new FutureAsync<>(result);
    }
    
    /**
     * Async that is rejected with the given throwable. Should typically not be used. Instead, throw exceptions from within async methods
     */
    static <T> Async<T> rejected(final Throwable t) {
        final FutureAsync<T> f = new FutureAsync<>();
        CompletableFutureUtil.reject(f, t);
        return f;
    }
    
    /**
     * Wait for and return the asynchronous process result. Should be used directly on a method call result. Avoid calling with a local variable
     * parameter to avoid misuse since the declared exceptions of the asynchronous method are really thrown by this method.
     *
     * <p>Can only be used in methods that return Async
     */
    static <T> T await(final CompletionStage<T> async) {
        throw new UnsupportedOperationException(
            "Only allowed in async methods (that return Async<?> or are annotated with @Async and return CompletionStage<?>). Use AsyncTransformer to transform this code");
    }
    
    /**
     * Similar to await() but returns the Async object instead, intended for returning the same result in case of success while recovering from
     * thrown exceptions. Should be used directly on a method call result. Avoid calling with a local variable parameter to avoid misuse since
     * the declared exceptions of the asynchronous method are really thrown by this method.
     *
     * <p>Can only be used in methods that return Async
     */
    static <T, U extends CompletionStage<T>> U awaitExceptions(final U async) {
        throw new UnsupportedOperationException(
            "Only allowed in async methods (that return Async<?> or are annotated with @Async and return CompletionStage<?>). Use AsyncTransformer to transform this code");
    }
    
    /**
     * @deprecated use awaitExceptions instead
     */
    @Deprecated
    static <T, U extends CompletionStage<T>> U awaitResult(final U async) {
        throw new UnsupportedOperationException(
            "Only allowed in async methods (that return Async<?> or are annotated with @Async and return CompletionStage<?>). Use AsyncTransformer to transform this code");
    }
    
    /**
     * @deprecated No longer needed as {@link Async} extends {@link CompletionStage}
     */
    @Deprecated
    static <T> CompletionStage<T> async(final Async<T> async) {
        return async;
    }
    
    /**
     * @deprecated Use {@link #from(CompletionStage)}
     */
    @Deprecated
    static <T> Async<T> async(final CompletionStage<T> stage) {
        return from(stage);
    }
    
    /**
     * Interop method to ignore exceptions in the signature since these will not really be thrown but cause the completion stage to be rejected
     *
     * @param async the async
     * @return the completion stage
     */
    @SuppressWarnings("squid:S1181")
    static <T> Async<T> from(final CompletionStageCallableThrows<T, ?> async) {
        try {
            return from(async.call());
        } catch (final Throwable t) {
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
        } else if (stage != null) {
            return new DelegatingAsync<>(stage);
        } else {
            return null;
        }
    }
    
    /**
     * @deprecated Use {@link CompletionStageUtil#block(CompletionStage)}
     */
    @Deprecated
    static <T> T block(final CompletionStage<T> async) throws InterruptedException {
        return CompletionStageUtil.block(async);
    }
    
    /**
     * @deprecated Use {@link CompletionStageUtil#block(CompletionStage, long, TimeUnit)}
     */
    @Deprecated
    static <T> T block(final CompletionStage<T> async, final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
        return CompletionStageUtil.block(async, timeout, unit);
    }
    
    /**
     * @deprecated use {@link #all(CreateArray, CompletionStage[])} 
     */
    @Deprecated
    @SafeVarargs
    static <T> Async<List<T>> allSame(final CompletionStage<T>... asyncs) {
        return all(Arrays.asList(asyncs));
    }
    
    /**
     * @deprecated use {@link #all(List)}
     */
    @Deprecated
    static <T> Async<List<T>> allSame(final List<? extends CompletionStage<? extends T>> asyncs) {
        return all(asyncs);
    }
    
    /**
     * @deprecated use {@link #all(Map)}
     */
    @Deprecated
    static <K, V> Async<Map<K, V>> allSame(final Map<K, ? extends CompletionStage<? extends V>> asyncs) {
        return all(asyncs);
    }
    
    /**
     * @see CompletionStageUtil#all(CreateArray, CompletionStage[])
     */
    @SafeVarargs
    static <T> Async<T[]> all(final CreateArray<T> createArray, final CompletionStage<? extends T>... asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.all(createArray, asyncs));
    }
    
    /**
     * @see CompletionStageUtil#all(List)
     */
    static <T> Async<List<T>> all(final List<? extends CompletionStage<? extends T>> asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.all(asyncs));
    }
    
    /**
     * @see CompletionStageUtil#all(Map)
     */
    static <K, V> Async<Map<K, V>> all(final Map<K, ? extends CompletionStage<? extends V>> asyncs) {
        return new DelegatingAsync<>(CompletionStageUtil.all(asyncs));
    }
    
    /**
     * @see CompletionStageUtil#all(CompletionStage, CompletionStage)
     */
    static <T1, T2> Async<Tuple2<T1, T2>> all(final CompletionStage<T1> p1, final CompletionStage<T2> p2) {
        return new DelegatingAsync<>(CompletionStageUtil.all(p1, p2));
    }
    
    /**
     * @see CompletionStageUtil#all(CompletionStage, CompletionStage, CompletionStage)
     */
    static <T1, T2, T3> Async<Tuple3<T1, T2, T3>> all(final CompletionStage<T1> p1, final CompletionStage<T2> p2, final CompletionStage<T3> p3) {
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
     * use to throw a consistent error indicating that code was not transformed. Typically not used by code unless it is some low level
     * infrastructure code, e.g. proxying calls
     */
    static UnsupportedOperationException noTransformation() {
        return new UnsupportedOperationException("Transform this code using AsyncTransformer");
    }
    
    /**
     * Acts like {@link CompletionStage#thenApply(Function)} but allows function to throw any exception, and joins the trace in case of failure
     *
     * @param function the mapping function
     * @param <U> the mapped type
     * @param <E> the type of the failure exception
     * @return the mapped stage
     */
    default <U, E extends Exception> Async<U> map(final FunctionThrows<T, U, E> function) throws E {
        return CompletionStageUtil.map(this, function);
    }
    
    default <E extends Exception> Async<T> onException(final Consumer<Throwable> consumer) throws E {
        return CompletionStageUtil.onException(this, consumer);
    }
    
}
