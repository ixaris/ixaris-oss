package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.AsyncExecutor.exec;
import static com.ixaris.commons.async.lib.AsyncExecutor.execSync;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.awaitility.Awaitility;
import org.junit.Test;

import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;

public class AsyncTraceTest {
    
    @Test
    public void testLogging() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> c = exec(ex, () -> execute(ex, 2));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(c));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(c))
            .satisfies(e -> new ThrowableAssert(CompletionStageUtil.extractCause(e))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(AsyncTrace.class)
                .satisfies(c1 -> new ThrowableAssert(c1.getCause())
                    .hasCauseInstanceOf(AsyncTrace.class)
                    .satisfies(c2 -> new ThrowableAssert(c2.getCause())
                        .hasCauseInstanceOf(AsyncTrace.class)
                        .satisfies(c3 -> new ThrowableAssert(c3.getCause())
                            .hasNoCause()))));
    }
    
    private Async<Void> execute(final Executor ex, final int depth) {
        if (depth == 0) {
            throw new IllegalStateException();
        } else {
            return exec(ex, () -> execute(ex, depth - 1));
        }
    }
    
    @Test
    public void testLoggingFromAsync() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> c = exec(ex, () -> throwAfterAwait(ex));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(c));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(c))
            .isInstanceOf(Throwable.class)
            .satisfies(e -> new ThrowableAssert(e)
                .hasCauseInstanceOf(AsyncTrace.class));
    }
    
    @Test
    public void testLoggingFromAsyncMap() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> c = exec(ex, () -> throwInMap(ex));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(c));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(c))
            .isInstanceOf(Throwable.class)
            .satisfies(e -> new ThrowableAssert(e)
                .hasCauseInstanceOf(AsyncTrace.class));
    }
    
    private Async<Void> throwAfterAwait(final Executor ex) {
        await(dummy(ex));
        throw new IllegalStateException("Expected");
    }
    
    private Async<Void> throwInMap(final Executor ex) {
        return dummy(ex).map(r -> {
            throw new IllegalStateException("Expected");
        });
    }
    
    private Async<Void> dummy(final Executor ex) {
        return execSync(ex, () -> null);
    }
    
}
