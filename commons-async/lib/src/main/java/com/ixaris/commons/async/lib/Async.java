package com.ixaris.commons.async.lib;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.misc.lib.object.Tuple4;
import com.ixaris.commons.misc.lib.object.Tuple5;

/**
 * <ol>
 * <li>Do not use Async types as parameters
 * <li>Only call await() from methods that return Async
 * <li>Be mindful that exceptions are really thrown by the await() call</li>
  * </ol>
 *
 * @param <T>
 */
public final class Async<T> {
    
    /**
     * @return the result holder, transformed to CompletionStage<T>
     */
    public static Async<Void> result() {
        throw wrongUsage();
    }
    
    /**
     * @param result the asynchronous process result
     * @param <T>
     * @return the result holder, transformed to CompletionStage<T>
     */
    public static <T> Async<T> result(final T result) {
        throw wrongUsage();
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
    public static <T> T await(final Async<T> async) {
        throw wrongUsage();
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
    public static <T> Async<T> awaitResult(final Async<T> async) {
        throw wrongUsage();
    }
    
    /**
     * Interop method to convert from Async<T> to CompletionStage<T>
     *
     * @param async the async
     * @return the completion stage
     */
    public static <T> CompletionStage<T> async(final Async<T> async) {
        throw wrongUsage();
    }
    
    /**
     * Interop method to convert from CompletionStage<T> to Async<T>
     *
     * @param stage the completion stage
     * @return the async
     */
    public static <T> Async<T> async(final CompletionStage<T> stage) {
        throw wrongUsage();
    }
    
    /**
     * Try not to use this method, except maybe in tests!
     * Blocks until the future is resolved
     *
     * @param async the async
     * @return the result
     */
    public static <T> T block(final Async<T> async) throws InterruptedException {
        throw wrongUsage();
    }
    
    /**
     * Try not to use this method, except maybe in tests!
     * Blocks until the future is resolved
     *
     * @param async the async
     * @param timeout
     * @param unit
     * @return the result
     */
    public static <T> T block(final Async<T> async, final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
        throw wrongUsage();
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
    public static <T> Async<List<T>> allSame(final Async<T>... asyncs) {
        throw wrongUsage();
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for results of the same type
     *
     * @return a future resolved with a list of results in the same order as the futures argument,
     *         or rejected with the first failure from the given futures
     */
    public static <T> Async<List<T>> allSame(final List<Async<T>> asyncs) {
        throw wrongUsage();
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails.
     * This variant is for results of the same type
     *
     * @return a future resolved with a map of results corresponding to the keys in the futures argument,
     *         or rejected with the first failure from the given futures
     */
    public static <K, V> Async<Map<K, V>> allSame(final Map<K, Async<V>> asyncs) {
        throw wrongUsage();
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for results of any type
     *
     * @return a future resolved with an array of results in the same order or the futures parameter,
     *         or rejected with the first failure from the given futures
     */
    public static Async<Object[]> all(final Async<?>... asyncs) {
        throw wrongUsage();
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for results of any type
     *
     * @return a future resolved with a list of results in the same order as the futures argument,
     *         or rejected with the first failure from the given futures
     */
    public static Async<List<Object>> all(final List<Async<?>> asyncs) {
        throw wrongUsage();
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails.
     * This variant is for results of the same type
     *
     * @return a future resolved with a map of results corresponding to the keys in the futures argument,
     *         or rejected with the first failure from the given futures
     */
    public static <K> Async<Map<K, ?>> all(final Map<K, Async<?>> asyncs) {
        throw wrongUsage();
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 2 results of any type
     */
    public static <T1, T2> Async<Tuple2<T1, T2>> all(final Async<T1> p1, final Async<T2> p2) {
        throw wrongUsage();
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 3 results of any type
     */
    public static <T1, T2, T3> Async<Tuple3<T1, T2, T3>> all(final Async<T1> p1,
                                                             final Async<T2> p2,
                                                             final Async<T3> p3) {
        throw wrongUsage();
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 4 results of any type
     */
    public static <T1, T2, T3, T4> Async<Tuple4<T1, T2, T3, T4>> all(final Async<T1> p1,
                                                                     final Async<T2> p2,
                                                                     final Async<T3> p3,
                                                                     final Async<T4> p4) {
        throw wrongUsage();
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 5 results of any type
     */
    public static <T1, T2, T3, T4, T5> Async<Tuple5<T1, T2, T3, T4, T5>> all(final Async<T1> p1,
                                                                             final Async<T2> p2,
                                                                             final Async<T3> p3,
                                                                             final Async<T4> p4,
                                                                             final Async<T5> p5) {
        throw wrongUsage();
    }
    
    /**
     * use to throw a consistent error indicating that code was not transformed. Typically not used by code unless
     * it is some low level infrastructure code, e.g. proxying calls
     */
    public static UnsupportedOperationException noTransformation() {
        return new UnsupportedOperationException("Transform this code using AsyncTransformer");
    }
    
    private static UnsupportedOperationException wrongUsage() {
        return new UnsupportedOperationException("Only allowed in methods that return Async<?>. Use AsyncTransformer to transform this code");
    }
    
    private Async() {
        throw wrongUsage();
    }
    
    /**
     * Synchronously map an async task result to another type. DO NOT BLOCK! To call further asynchronous tasks,
     * use await()
     *
     * @param function
     * @param <MAP_T>
     * @param <E>
     * @return
     * @throws E
     */
    public <MAP_T, E extends Exception> Async<MAP_T> map(final FunctionThrows<T, MAP_T, E> function) throws E {
        throw wrongUsage();
    }
    
}
