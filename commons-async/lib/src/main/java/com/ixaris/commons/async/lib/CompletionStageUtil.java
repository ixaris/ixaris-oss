package com.ixaris.commons.async.lib;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
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
        f.completeExceptionally(AsyncTrace.join(t));
        return f;
    }
    
    public static boolean isDone(final CompletionStage<?> stage) {
        return stage.toCompletableFuture().isDone();
    }
    
    public static boolean isFulfilled(final CompletionStage<?> stage) {
        final CompletableFuture<?> future = stage.toCompletableFuture();
        return future.isDone() && !future.isCompletedExceptionally();
    }
    
    public static boolean isRejected(final CompletionStage<?> stage) {
        return stage.toCompletableFuture().isCompletedExceptionally();
    }
    
    public static <T> T get(final CompletionStage<T> stage) {
        try {
            return stage.toCompletableFuture().getNow(null);
        } catch (final CompletionException e) {
            throw ExceptionUtil.sneakyThrow(e.getCause());
        }
    }
    
    public static <T> T block(final CompletionStage<T> stage) throws InterruptedException {
        try {
            return stage.toCompletableFuture().get();
        } catch (final ExecutionException e) {
            throw ExceptionUtil.sneakyThrow(e.getCause());
        }
    }
    
    public static <T> T block(final CompletionStage<T> stage, final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            return stage.toCompletableFuture().get(timeout, unit);
        } catch (final ExecutionException e) {
            throw ExceptionUtil.sneakyThrow(e.getCause());
        }
    }
    
    public static <T, U> CompletionStage<U> map(final CompletionStage<T> stage,
                                                final FunctionThrows<T, U, ? extends Throwable> function) {
        return stage.thenApply(r -> {
            try {
                return function.apply(r);
            } catch (final Throwable t) { // NOSONAR framework code needs to catch everything
                throw ExceptionUtil.sneakyThrow(AsyncTrace.join(t));
            }
        });
    }
    
    public static <T, U> CompletionStage<U> doneCompose(final CompletionStage<T> stage,
                                                        final FunctionThrows<CompletionStage<T>, CompletionStage<U>, ? extends Throwable> function) {
        return stage.exceptionally(t -> null).thenCompose(r -> {
            try {
                return function.apply(stage);
            } catch (final Throwable t) { // NOSONAR framework code needs to catch everything
                throw ExceptionUtil.sneakyThrow(AsyncTrace.join(t));
            }
        });
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails.
     * This variant is for results of the same type
     * <p>
     * returns List instead of array due to java's inability to create a generic array. Alternative signature
     * would be &lt;T&gt; CompletionStage&lt;T[]&gt; allSame(CompletionStage&lt;T&gt;... stages, Class&lt;T&gt; type)
     * and use (T[]) Array.newInstance(type, capacity) to create the result array
     *
     * @return a future resolved with a list of results in the same order as the futures argument,
     *         or rejected with the first failure from the given futures
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> CompletionStage<List<T>> allSame(final CompletionStage<T>... stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages is null");
        }
        if (stages.length == 0) {
            return fulfilled(Collections.emptyList());
        }
        final CompletableFuture<T>[] futures = (CompletableFuture<T>[]) new CompletableFuture<?>[stages.length];
        for (int l = stages.length, i = 0; i < l; i++) {
            futures[i] = stages[i].toCompletableFuture();
        }
        return allSame(futures);
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for results of the same type
     *
     * @return a future resolved with a list of results in the same order as the futures argument,
     *         or rejected with the first failure from the given futures
     */
    @SuppressWarnings("unchecked")
    public static <T> CompletionStage<List<T>> allSame(final List<CompletionStage<T>> stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages is null");
        }
        if (stages.isEmpty()) {
            return fulfilled(Collections.emptyList());
        }
        final CompletableFuture<T>[] futures = (CompletableFuture<T>[]) new CompletableFuture<?>[stages.size()];
        int i = 0;
        for (final CompletionStage<T> stage : stages) {
            futures[i++] = stage.toCompletableFuture();
        }
        return allSame(futures);
    }
    
    private static <T> CompletionStage<List<T>> allSame(final CompletableFuture<T>[] futures) {
        return CompletableFuture.allOf(futures).thenApply(r -> {
            final List<T> results = new ArrayList<>(futures.length);
            for (int l = futures.length, i = 0; i < l; i++) {
                try {
                    results.add(futures[i].getNow(null));
                } catch (final CompletionException e) {
                    throw ExceptionUtil.sneakyThrow(e.getCause());
                }
            }
            return results;
        });
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails.
     * This variant is for results of the same type
     *
     * @return a future resolved with a map of results corresponding to the keys in the futures argument,
     *         or rejected with the first failure from the given futures
     */
    @SuppressWarnings("unchecked")
    public static <K, V> CompletionStage<Map<K, V>> allSame(final Map<K, CompletionStage<V>> stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages is null");
        }
        if (stages.isEmpty()) {
            return fulfilled(Collections.emptyMap());
        }
        final CompletableFuture<Tuple2<K, V>>[] futures = (CompletableFuture<Tuple2<K, V>>[]) new CompletableFuture<?>[stages.size()];
        int i = 0;
        for (final Map.Entry<K, CompletionStage<V>> stageEntry : stages.entrySet()) {
            futures[i++] = stageEntry.getValue().thenApply(v -> tuple(stageEntry.getKey(), v)).toCompletableFuture();
        }
        return CompletableFuture.allOf(futures).thenApply(r -> {
            final Map<K, V> results = new HashMap<>();
            for (int l = futures.length, j = 0; j < l; j++) {
                try {
                    final Tuple2<K, V> tuple = futures[j].getNow(null);
                    results.put(tuple.get1(), tuple.get2());
                } catch (final CompletionException e) {
                    throw ExceptionUtil.sneakyThrow(e.getCause());
                }
            }
            return results;
        });
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for results of any type
     *
     * @return a future resolved with an array of results in the same order or the futures parameter,
     *         or rejected with the first failure from the given futures
     */
    public static CompletionStage<Object[]> all(final CompletionStage<?>... stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages is null");
        }
        if (stages.length == 0) {
            return fulfilled(new Object[0]);
        }
        final CompletableFuture<?>[] futures = new CompletableFuture<?>[stages.length];
        for (int l = stages.length, i = 0; i < l; i++) {
            futures[i] = stages[i].toCompletableFuture();
        }
        return all(futures);
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for results of any type
     *
     * @return a future resolved with a list of results in the same order as the futures argument,
     *         or rejected with the first failure from the given futures
     */
    @SuppressWarnings("unchecked")
    public static CompletionStage<List<Object>> all(final List<? extends CompletionStage<?>> stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages is null");
        }
        if (stages.isEmpty()) {
            return fulfilled(Collections.emptyList());
        }
        final CompletableFuture<?>[] futures = new CompletableFuture<?>[stages.size()];
        int i = 0;
        for (final CompletionStage<?> stage : stages) {
            futures[i++] = stage.toCompletableFuture();
        }
        return all(futures).thenApply(Arrays::asList);
    }
    
    private static CompletionStage<Object[]> all(final CompletableFuture<?>[] futures) {
        return CompletableFuture.allOf(futures).thenApply(r -> {
            final Object[] results = new Object[futures.length];
            for (int l = futures.length, i = 0; i < l; i++) {
                try {
                    results[i] = futures[i].getNow(null);
                } catch (final CompletionException e) {
                    throw ExceptionUtil.sneakyThrow(e.getCause());
                }
            }
            return results;
        });
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails.
     * This variant is for results of the same type
     *
     * @return a future resolved with a map of results corresponding to the keys in the futures argument,
     *         or rejected with the first failure from the given futures
     */
    @SuppressWarnings("unchecked")
    public static <K> CompletionStage<Map<K, ?>> all(final Map<K, CompletionStage<?>> stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages is null");
        }
        if (stages.isEmpty()) {
            return fulfilled(Collections.emptyMap());
        }
        final CompletableFuture<Tuple2<K, Object>>[] futures = (CompletableFuture<Tuple2<K, Object>>[]) new CompletableFuture<?>[stages.size()];
        int i = 0;
        for (final Map.Entry<K, CompletionStage<?>> stageEntry : stages.entrySet()) {
            futures[i++] = stageEntry.getValue().thenApply(v -> tuple(stageEntry.getKey(), (Object) v)).toCompletableFuture();
        }
        return CompletableFuture.allOf(futures).thenApply(r -> {
            final Map<K, Object> results = new HashMap<>();
            for (int l = futures.length, j = 0; j < l; j++) {
                try {
                    final Tuple2<K, Object> tuple = futures[j].getNow(null);
                    results.put(tuple.get1(), tuple.get2());
                } catch (final CompletionException e) {
                    throw ExceptionUtil.sneakyThrow(e.getCause());
                }
            }
            return results;
        });
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 2 results of any type
     */
    @SuppressWarnings("unchecked")
    public static <T1, T2> CompletionStage<Tuple2<T1, T2>> all(final CompletionStage<T1> s1, final CompletionStage<T2> s2) {
        return all(new CompletionStage<?>[] { s1, s2 })
            .thenApply(r -> tuple((T1) r[0], (T2) r[1]));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 2 results of any type
     */
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3> CompletionStage<Tuple3<T1, T2, T3>> all(final CompletionStage<T1> s1,
                                                                       final CompletionStage<T2> s2,
                                                                       final CompletionStage<T3> s3) {
        return all(new CompletionStage<?>[] { s1, s2, s3 })
            .thenApply(r -> tuple((T1) r[0], (T2) r[1], (T3) r[2]));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 2 results of any type
     */
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4> CompletionStage<Tuple4<T1, T2, T3, T4>> all(final CompletionStage<T1> s1,
                                                                               final CompletionStage<T2> s2,
                                                                               final CompletionStage<T3> s3,
                                                                               final CompletionStage<T4> s4) {
        return all(new CompletionStage<?>[] { s1, s2, s3, s4 })
            .thenApply(r -> tuple((T1) r[0], (T2) r[1], (T3) r[2], (T4) r[3]));
    }
    
    /**
     * Combine multiple asynchronous result to one, resolved when all complete or one fails
     * This variant is for 2 results of any type
     */
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5> CompletionStage<Tuple5<T1, T2, T3, T4, T5>> all(final CompletionStage<T1> s1,
                                                                                       final CompletionStage<T2> s2,
                                                                                       final CompletionStage<T3> s3,
                                                                                       final CompletionStage<T4> s4,
                                                                                       final CompletionStage<T5> s5) {
        return all(new CompletionStage<?>[] { s1, s2, s3, s4, s5 })
            .thenApply(r -> tuple((T1) r[0], (T2) r[1], (T3) r[2], (T4) r[3], (T5) r[4]));
    }
    
    public static Throwable extractCause(final Throwable t) {
        return (t instanceof CompletionException) ? t.getCause() : t;
    }
    
    private CompletionStageUtil() {}
    
}
