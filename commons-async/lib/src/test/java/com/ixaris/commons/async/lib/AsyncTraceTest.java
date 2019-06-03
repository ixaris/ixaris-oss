package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.AsyncExecutor.exec;
import static com.ixaris.commons.async.lib.AsyncExecutor.execAndRelay;
import static com.ixaris.commons.async.lib.AsyncExecutor.relay;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

public class AsyncTraceTest {
    
    @Test
    public void testLogging() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> c = exec(ex, () -> execute(ex, 2));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(c));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(c))
            .satisfies(e ->
                new ThrowableAssert(CompletionStageUtil.extractCause(e))
                    .isInstanceOf(IllegalStateException.class)
                    .hasCauseInstanceOf(AsyncTrace.class)
                    .satisfies(c1 ->
                        new ThrowableAssert(c1.getCause())
                            .hasMessageContaining("Trace [2] @")
                            .hasCauseInstanceOf(AsyncTrace.class)
                            .satisfies(c2 ->
                                new ThrowableAssert(c2.getCause())
                                    .hasCauseInstanceOf(AsyncTrace.class)
                                    .hasMessageContaining("Trace [1] @")
                                    .satisfies(c3 ->
                                        new ThrowableAssert(c3.getCause())
                                            .hasMessageContaining("Trace [0] @")
                                            .hasNoCause()
                                    )
                            )
                    )
            );
    }
    
    private Async<Void> execute(final Executor ex, final int depth) {
        if (depth == 0) {
            throw new IllegalStateException();
        } else {
            return relay(exec(ex, () -> execute(ex, depth - 1)));
        }
    }
    
    @Test
    public void testLoggingFromAsync() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> cs = exec(ex, () -> throwAfterAwait(ex));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(cs));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(cs))
            .isInstanceOf(Throwable.class)
            .satisfies(e ->
                new ThrowableAssert(e)
                    .hasCauseInstanceOf(AsyncTrace.class)
                    .satisfies(c2 ->
                        new ThrowableAssert(c2.getCause())
                            .hasCauseInstanceOf(AsyncTrace.class)
                            .hasMessageContaining("Trace [1] @")
                            .satisfies(c3 ->
                                new ThrowableAssert(c3.getCause()).hasMessageContaining("Trace [0] @").hasNoCause()
                            )
                    )
            );
    }
    
    @Test
    public void testLoggingFromAsyncMap() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> cs = exec(ex, () -> throwInMap(ex));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(cs));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(cs))
            .isInstanceOf(Throwable.class)
            .satisfies(e ->
                new ThrowableAssert(e)
                    .hasCauseInstanceOf(AsyncTrace.class)
                    .satisfies(c2 ->
                        new ThrowableAssert(c2.getCause())
                            .hasCauseInstanceOf(AsyncTrace.class)
                            .hasMessageContaining("Trace [1] @")
                            .satisfies(c3 ->
                                new ThrowableAssert(c3.getCause()).hasMessageContaining("Trace [0] @").hasNoCause()
                            )
                    )
            );
    }
    
    @Test
    public void testLoggingFromLoop() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> cs = exec(ex, () -> throwInLoop(ex));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(cs));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(cs))
            .isInstanceOf(Throwable.class)
            .satisfies(e ->
                new ThrowableAssert(e)
                    .hasCauseInstanceOf(AsyncTrace.class)
                    .satisfies(c2 ->
                        new ThrowableAssert(c2.getCause())
                            .hasCauseInstanceOf(AsyncTrace.class)
                            .hasMessageContaining("Trace [99] @")
                    )
            );
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
    
    private Async<Void> throwInLoop(final Executor ex) {
        for (int i = 0; i < 100; i++) {
            await(dummy(ex));
            await(dummy(ex));
        }
        throw new IllegalStateException("Expected");
    }
    
    private Async<Void> dummy(final Executor ex) {
        return execAndRelay(ex, (CompletionStageCallableThrows<Void, RuntimeException>) Async::result);
    }
    
}
