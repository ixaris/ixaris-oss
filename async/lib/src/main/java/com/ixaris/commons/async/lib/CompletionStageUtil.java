package com.ixaris.commons.async.lib;

import static com.ixaris.commons.misc.lib.exception.ExceptionUtil.sneakyThrow;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.object.Reference;
import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.misc.lib.object.Tuple4;
import com.ixaris.commons.misc.lib.object.Tuple5;

/**
 * Utility methods around {@link CompletionStage} to remove some complexity from Transformer
 */
public final class CompletionStageUtil {
    
    private static final CompletionStage<Void> COMPLETED = CompletableFuture.completedFuture(null);
    
    public static CompletionStage<Void> fulfilled() {
        return COMPLETED;
    }
    
    public static <T> CompletionStage<T> fulfilled(final T result) {
        return CompletableFuture.completedFuture(result);
    }
    
    public static <T> CompletionStage<T> rejected(final Throwable t) {
        final CompletableFuture<T> f = new CompletableFuture<>();
        CompletableFutureUtil.reject(f, t);
        return f;
    }
    
    /**
     * @return true if the given stage is done (fulfilled or rejected)
     */
    public static boolean isDone(final CompletionStage<?> stage) {
        return stage.toCompletableFuture().isDone();
    }
    
    /**
     * Shortcuts whenComplete (and the corresponding future creation) if the future is done
     */
    @SuppressWarnings({ "squid:S1181", "squid:S1166", "findbugs:NP_NONNULL_PARAM_VIOLATION" })
    public static <T> CompletionStage<T> whenDone(final CompletionStage<T> stage, final BiConsumer<? super T, ? super Throwable> action) {
        final CompletableFuture<T> future = stage.toCompletableFuture();
        if (future.isDone()) {
            try {
                action.accept(future.getNow(null), null);
            } catch (final CompletionException e) {
                action.accept(null, e.getCause());
            } catch (final Throwable t) {
                action.accept(null, t);
            }
            return stage;
        } else {
            return stage.whenComplete(action);
        }
    }
    
    /**
     * @return true if the given stage is fulfilled
     */
    public static boolean isFulfilled(final CompletionStage<?> stage) {
        final CompletableFuture<?> future = stage.toCompletableFuture();
        return future.isDone() && !future.isCompletedExceptionally();
    }
    
    /**
     * @return true if the given stage is rejected
     */
    public static boolean isRejected(final CompletionStage<?> stage) {
        return stage.toCompletableFuture().isCompletedExceptionally();
    }
    
    /**
     * Gets the resolved value of a stage, or throws the failure exception. Will return null if the stage is not yet complete, so it should only
     * be used in combination with {@link #isDone(CompletionStage)}
     */
    @SuppressWarnings("findbugs:NP_NONNULL_PARAM_VIOLATION")
    public static <T> T get(final CompletionStage<T> stage) {
        try {
            return stage.toCompletableFuture().getNow(null);
        } catch (final CompletionException e) {
            throw sneakyThrow(e.getCause());
        }
    }
    
    /**
     * Do not use this method unless you're sure that the stage is completed or that you really want to block! Blocks until the stage is resolved
     * and returns the result or throws the failure exception (which may be any Throwable subclass)
     */
    public static <T> T join(final CompletionStage<T> stage) {
        if (!AsyncExecutor.get().isAllowJoin()) {
            throw new UnsupportedOperationException("Executor does not allow join()");
        }
        
        try {
            return stage.toCompletableFuture().join();
        } catch (final CompletionException e) {
            throw sneakyThrow(e.getCause());
        }
    }
    
    /**
     * Try not to use this method, except maybe in tests! Blocks until the stage is resolved and returns the result or throws the failure
     * exception (which may be any Throwable subclass)
     *
     * @param stage the stage
     * @return the result
     */
    public static <T> T block(final CompletionStage<T> stage) throws InterruptedException {
        if (!AsyncExecutor.get().isAllowJoin()) {
            throw new UnsupportedOperationException("Executor does not allow block()");
        }
        
        try {
            return stage.toCompletableFuture().get();
        } catch (final ExecutionException e) {
            throw sneakyThrow(e.getCause());
        }
    }
    
    /**
     * Try not to use this method, except maybe in tests! Blocks until the stage is resolved and returns the result or throws the failure
     * exception (which may be any Throwable subclass), or throws TimeoutException if time runs out
     *
     * @param stage the stage
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the result
     */
    public static <T> T block(final CompletionStage<T> stage, final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            return stage.toCompletableFuture().get(timeout, unit);
        } catch (final ExecutionException e) {
            throw sneakyThrow(e.getCause());
        }
    }
    
    /**
     * composes a stage to another stage on completion, an action that is missing in the {@link CompletionStage} API. This is also used by the
     * continuations generated by the transformer.
     */
    @SuppressWarnings("squid:S1181")
    public static <T, U, S extends CompletionStage<T>> CompletionStage<U> doneCompose(final S stage,
                                                                                      final FunctionThrows<S, ? extends CompletionStage<U>, ? extends Exception> function) {
        final FunctionThrows<S, ? extends CompletionStage<U>, ? extends Exception> wrapped = AsyncLocal.wrapThrows(function);
        return stage
            .exceptionally(t -> null)
            .thenCompose(r -> {
                try {
                    return wrapped.apply(stage);
                } catch (final Throwable t) {
                    throw sneakyThrow(AsyncTrace.join(t));
                }
            });
    }
    
    /**
     * Acts like {@link CompletionStage#thenApply(Function)} but allows function to throw any exception, and joins the
     * trace in case of failure
     *
     * @param function the mapping function
     * @param <U> the mapped type
     * @param <E> the type of the failure exception
     * @return the mapped stage
     */
    @SuppressWarnings("squid:S1181")
    public static <T, U, E extends Exception> Async<U> map(final CompletionStage<T> stage, final FunctionThrows<T, U, E> function) throws E {
        final FunctionThrows<T, U, E> wrapped = AsyncLocal.wrapThrows(function);
        return new DelegatingAsync<>(stage.thenApply(t -> {
            try {
                return wrapped.apply(t);
            } catch (final Throwable tt) {
                throw sneakyThrow(AsyncTrace.join(tt));
            }
        }));
    }
    
    @SuppressWarnings("squid:S1181")
    public static <T, E extends Exception> Async<T> onException(final CompletionStage<T> stage, final Consumer<Throwable> consumer) throws E {
        final Consumer<Throwable> wrapped = AsyncLocal.wrap(consumer);
        return new DelegatingAsync<>(stage.exceptionally(t -> {
            try {
                wrapped.accept(AsyncTrace.join(t));
                throw sneakyThrow(t);
            } catch (final Throwable tt) {
                throw sneakyThrow(AsyncTrace.join(tt));
            }
        }));
    }
    
    @FunctionalInterface
    public interface CreateArray<T> {
        
        T[] create(int length);
        
    }
    
    public static CompletionStage<Object[]> all(final CompletionStage<?>... stages) {
        return all(CREATE_OBJECT_ARRAY, stages);
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails. This variant is for results of the same type
     *
     * @return a future resolved with a list of results in the same order as the futures argument, or rejected with the first failure from the
     *     given futures
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> CompletionStage<T[]> all(final CreateArray<T> createArray, final CompletionStage<? extends T>... stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages is null");
        }
        if (stages.length == 0) {
            return fulfilled(createArray.create(0));
        }
        final CompletableFuture<? extends T>[] futures = new CompletableFuture[stages.length];
        for (int l = stages.length, i = 0; i < l; i++) {
            futures[i] = stages[i].toCompletableFuture();
        }
        shortcutFutureArray(futures);
        
        return CompletableFuture
            .allOf(futures)
            .thenApply(a -> {
                final T[] results = createArray.create(futures.length);
                for (int l = futures.length, i = 0; i < l; i++) {
                    results[i] = futures[i].join();
                }
                return results;
            });
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails This variant is for results of the same type
     *
     * @return a future resolved with a list of results in the same order as the futures argument, or rejected with the first failure from the
     *     given futures
     */
    @SuppressWarnings("unchecked")
    public static <T> CompletionStage<List<T>> all(final List<? extends CompletionStage<? extends T>> stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages is null");
        }
        if (stages.isEmpty()) {
            return fulfilled(Collections.emptyList());
        }
        final CompletableFuture<? extends T>[] futures = new CompletableFuture[stages.size()];
        int x = 0;
        for (final CompletionStage<? extends T> stage : stages) {
            futures[x] = stage.toCompletableFuture();
            x++;
        }
        shortcutFutureArray(futures);
        
        return CompletableFuture
            .allOf(futures)
            .thenApply(a -> {
                final List<T> results = new ArrayList<>(futures.length);
                for (CompletableFuture<? extends T> future : futures) {
                    results.add(future.join());
                }
                return results;
            });
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails. This variant is for results of the same type
     *
     * @return a future resolved with a map of results corresponding to the keys in the futures argument, or rejected with the first failure from
     *     the given futures
     */
    @SuppressWarnings("unchecked")
    public static <K, V> CompletionStage<Map<K, V>> all(final Map<K, ? extends CompletionStage<? extends V>> stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages is null");
        }
        if (stages.isEmpty()) {
            return fulfilled(Collections.emptyMap());
        }
        final CompletableFuture<? extends Tuple2<K, ? extends V>>[] futures = new CompletableFuture[stages.size()];
        int x = 0;
        for (final Entry<K, ? extends CompletionStage<? extends V>> stageEntry : stages.entrySet()) {
            futures[x] = stageEntry.getValue().thenApply(v -> tuple(stageEntry.getKey(), v)).toCompletableFuture();
            x++;
        }
        shortcutFutureArray(futures);
        
        return CompletableFuture
            .allOf(futures)
            .thenApply(r -> {
                final Map<K, V> results = new HashMap<>();
                for (int l = futures.length, j = 0; j < l; j++) {
                    try {
                        final Tuple2<K, ? extends V> tuple = futures[j].join();
                        results.put(tuple.get1(), tuple.get2());
                    } catch (final CompletionException e) {
                        throw sneakyThrow(AsyncTrace.join(e.getCause()));
                    }
                }
                return results;
            });
    }
    
    private static final CreateArray<Object> CREATE_OBJECT_ARRAY = Object[]::new;
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails This variant is for 2 results of any type
     *
     * @return a future resolved with a tuple of results corresponding to the futures argument, or rejected with the first failure from the given
     *     futures
     */
    @SuppressWarnings("unchecked")
    public static <T1, T2> CompletionStage<Tuple2<T1, T2>> all(final CompletionStage<T1> s1, final CompletionStage<T2> s2) {
        return all(new CompletionStage<?>[] { s1, s2 }).thenApply(r -> tuple((T1) r[0], (T2) r[1]));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails This variant is for 3 results of any type
     *
     * @return a future resolved with a tuple of results corresponding to the futures argument, or rejected with the first failure from the given
     *     futures
     */
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3> CompletionStage<Tuple3<T1, T2, T3>> all(final CompletionStage<T1> s1,
                                                                       final CompletionStage<T2> s2,
                                                                       final CompletionStage<T3> s3) {
        return all(new CompletionStage<?>[] { s1, s2, s3 }).thenApply(r -> tuple((T1) r[0], (T2) r[1], (T3) r[2]));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails This variant is for 4 results of any type
     *
     * @return a future resolved with a tuple of results corresponding to the futures argument, or rejected with the first failure from the given
     *     futures
     */
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4> CompletionStage<Tuple4<T1, T2, T3, T4>> all(final CompletionStage<T1> s1,
                                                                               final CompletionStage<T2> s2,
                                                                               final CompletionStage<T3> s3,
                                                                               final CompletionStage<T4> s4) {
        return all(new CompletionStage<?>[] { s1, s2, s3, s4 }).thenApply(r -> tuple((T1) r[0], (T2) r[1], (T3) r[2], (T4) r[3]));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails This variant is for 5 results of any type
     *
     * @return a future resolved with a tuple of results corresponding to the futures argument, or rejected with the first failure from the given
     *     futures
     */
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5> CompletionStage<Tuple5<T1, T2, T3, T4, T5>> all(final CompletionStage<T1> s1,
                                                                                       final CompletionStage<T2> s2,
                                                                                       final CompletionStage<T3> s3,
                                                                                       final CompletionStage<T4> s4,
                                                                                       final CompletionStage<T5> s5) {
        return all(new CompletionStage<?>[] { s1, s2, s3, s4, s5 }).thenApply(r -> tuple((T1) r[0], (T2) r[1], (T3) r[2], (T4) r[3], (T5) r[4]));
    }
    
    /**
     * Fails on the first failure from the futures list rather than the first one temporally, i.e. given that the
     * futures may execute in parallel, rather than fail on the first one (time wise) that fails, this fails when a
     * failure is preceeded by successes in the ordered list. e.g. given 2 futures F0 and F1, if F1 fails, the failure
     * is not reported until F0 is resolved. If F0 is successful, F1 failure is reported, otherwise, F0 failure is
     * reported
     */
    @SuppressWarnings("unchecked")
    public static <T> CompletionStage<List<T>> allOrderedFirstFailure(final List<? extends CompletionStage<? extends T>> stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages is null");
        }
        if (stages.isEmpty()) {
            return fulfilled(Collections.emptyList());
        }
        
        final boolean[] success = new boolean[stages.size()];
        final Reference.Integer firstFailureIndex = new Reference.Integer(-1);
        final Reference<Throwable> firstFailure = new Reference<>();
        
        final CompletableFuture<? extends T>[] futures = new CompletableFuture[stages.size()];
        int x = 0;
        for (final CompletionStage<? extends T> stage : stages) {
            final int index = x;
            futures[x] =
                stage
                    .thenApply(r -> {
                        synchronized (success) {
                            success[index] = true;
                            if (index < firstFailureIndex.get()) {
                                throwIfAllPrevSuccess(success, firstFailureIndex.get(), firstFailure.get());
                            }
                        }
                        return r;
                    })
                    .exceptionally(e -> {
                        synchronized (success) {
                            if ((firstFailureIndex.get() == -1) || (index < firstFailureIndex.get())) {
                                firstFailureIndex.set(index);
                                firstFailure.set(e);
                                throwIfAllPrevSuccess(success, index, e);
                            }
                        }
                        return null;
                    })
                    .toCompletableFuture();
            x++;
        }
        shortcutFutureArray(futures);
        
        return CompletableFuture
            .allOf(futures)
            .thenApply(a -> {
                final List<T> results = new ArrayList<>(futures.length);
                for (CompletableFuture<? extends T> future : futures) {
                    results.add(future.join());
                }
                return results;
            });
    }
    
    /**
     * Extract the cause of a future failure, which will be wrapped in a {@link CompletionException} if it is a checked exception (except when
     * calling get() where the failure will be wrapped in an {@link ExecutionException}
     */
    public static Throwable extractCause(final Throwable t) {
        return (t instanceof CompletionException) ? t.getCause() : t;
    }
    
    private static void throwIfAllPrevSuccess(final boolean[] success, final int failureIndex, final Throwable e) {
        boolean allSuccess = true;
        for (int j = 0; j < failureIndex; j++) {
            if (!success[j]) {
                allSuccess = false;
                break;
            }
        }
        if (allSuccess) {
            throw sneakyThrow(e);
        }
    }
    
    private static void shortcutFutureArray(final CompletableFuture<?>[] futures) {
        for (int l = futures.length, i = 0; i < l; i++) {
            futures[i].exceptionally(e -> {
                for (final CompletableFuture<?> completableFuture : futures) {
                    completableFuture.complete(null);
                }
                throw sneakyThrow(e);
            });
        }
    }
    
    private CompletionStageUtil() {}
    
}
