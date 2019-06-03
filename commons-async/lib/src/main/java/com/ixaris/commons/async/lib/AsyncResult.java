package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.map;
import static com.ixaris.commons.misc.lib.exception.ExceptionUtil.sneakyThrow;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Use this class when joining a number of async tasks, and you need the result / throwable of each async process. Use
 * one of the convert() overloads to convert a collection of futures before calling one of the all() overloads
 */
public final class AsyncResult<T, E extends Throwable> {
    
    @SuppressWarnings("unchecked")
    public static <T> Async<AsyncResult<T, RuntimeException>>[] convert(final CompletionStage<T>[] array) {
        final Async<AsyncResult<T, RuntimeException>>[] converted = new Async[array.length];
        for (int l = array.length, i = 0; i < l; i++) {
            converted[i] = convert(array[i]);
        }
        return converted;
    }
    
    public static <T> List<Async<AsyncResult<T, RuntimeException>>> convert(
        final List<? extends CompletionStage<T>> stage
    ) {
        return stage.stream().map(AsyncResult::convert).collect(Collectors.toList());
    }
    
    public static <K, V> Map<K, Async<AsyncResult<V, RuntimeException>>> convert(
        final Map<K, ? extends CompletionStage<V>> map
    ) {
        return map.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> AsyncResult.convert(e.getValue())));
    }
    
    @SuppressWarnings("squid:S1181")
    public static <T> Async<AsyncResult<T, RuntimeException>> convert(final CompletionStage<T> stage) {
        try {
            return map(awaitExceptions(stage), r -> new AsyncResult<>(r, null));
        } catch (final Throwable t) {
            return result(new AsyncResult<>(null, t));
        }
    }
    
    @SuppressWarnings("squid:S1181")
    public static <T, E extends Throwable> Async<AsyncResult<T, E>> convert(
        final CompletionStageCallableThrows<T, E> callable
    ) {
        try {
            return map(awaitExceptions(callable.call()), r -> new AsyncResult<>(r, null));
        } catch (final Throwable t) {
            return result(new AsyncResult<>(null, t));
        }
    }
    
    private final T result;
    private final Throwable throwable;
    
    private AsyncResult(final T result, final Throwable throwable) {
        this.result = result;
        this.throwable = throwable;
    }
    
    public boolean hasResult() {
        return throwable == null;
    }
    
    public T get() throws E {
        if (throwable != null) {
            throw sneakyThrow(throwable);
        }
        return result;
    }
    
    public Throwable getThrowable() {
        return throwable;
    }
    
}
