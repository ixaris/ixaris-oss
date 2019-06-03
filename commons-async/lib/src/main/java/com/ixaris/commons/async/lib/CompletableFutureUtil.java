package com.ixaris.commons.async.lib;

import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CompletableFutureUtil {
    
    /**
     * Relay the result of a callable to a completable future
     *
     * @param future the future to be completed with the result / exception of the relay stage
     * @param callable the callable whose result or exception to relay
     */
    public static <T> void complete(final CompletableFuture<T> future, final CallableThrows<T, ?> callable) {
        try {
            future.complete(callable.call());
        } catch (final Throwable t) { // NOSONAR future handling
            future.completeExceptionally(AsyncTrace.join(t));
        }
    }
    
    /**
     * Relay the result of a runnableThrows to a completable future
     *
     * @param future the future to be completed with the result / exception of the relay stage
     * @param runnable the runnable whose result or exception to relay
     */
    public static void complete(final CompletableFuture<Void> future, final RunnableThrows<?> runnable) {
        try {
            runnable.run();
            future.complete(null);
        } catch (final Throwable t) { // NOSONAR future handling
            future.completeExceptionally(AsyncTrace.join(t));
        }
    }
    
    /**
     * Relay the result of a callable to a completable future
     *
     * @param future the future to be completed with the result / exception
     * @param r the result
     * @param t exception
     */
    public static <T> void complete(final CompletableFuture<T> future, final T r, final Throwable t) {
        if (t != null) {
            future.completeExceptionally(AsyncTrace.join(CompletionStageUtil.extractCause(t)));
        } else {
            future.complete(r);
        }
    }
    
    /**
     * Relay the result of a completion stage to a completable future
     *
     * @param future the future to be completed with the result / exception of the given stage
     * @param callable the callable from which to obtain the stage from which to complete
     */
    public static <T> void complete(
        final CompletableFuture<T> future, final CompletionStageCallableThrows<T, ?> callable
    ) {
        try {
            complete(future, callable.call());
        } catch (final Throwable t) { // NOSONAR future handling
            future.completeExceptionally(AsyncTrace.join(t));
        }
    }
    
    /**
     * Relay the result of a completion stage to a completable future
     *
     * @param future the future to be completed with the result / exception of the given stage
     * @param stage the stage to complete from
     */
    public static <T> void complete(final CompletableFuture<T> future, final CompletionStage<T> stage) {
        CompletionStageUtil.whenDone(stage, (r, t) -> complete(future, r, t));
    }
    
    /**
     * Reject a future, joining the trace
     *
     * @param future
     * @param t
     */
    public static void reject(final CompletableFuture<?> future, final Throwable t) {
        future.completeExceptionally(AsyncTrace.join(t));
    }
    
    private CompletableFutureUtil() {}
    
}
