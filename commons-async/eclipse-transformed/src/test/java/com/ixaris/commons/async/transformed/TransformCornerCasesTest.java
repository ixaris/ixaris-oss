package com.ixaris.commons.async.transformed;

import static com.ixaris.commons.async.lib.Async.async;
import static com.ixaris.commons.async.lib.Async.block;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.ixaris.commons.async.lib.AsyncTrace;
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;

public class TransformCornerCasesTest {
    
    @Test
    public void testAsyncProcessCanWorkWithSingleThread() throws Exception {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final TransformCornerCases tt = new TransformCornerCases();
        
        final CompletableFuture<Long> future = new CompletableFuture<>();
        ex.execute(AsyncTrace.wrap((Runnable) () -> async(tt.operation()).whenComplete((r, t) -> {
            if (t == null) {
                future.complete(r);
            } else {
                future.completeExceptionally(AsyncTrace.join(t));
            }
        })));
        future.get(1000, TimeUnit.SECONDS);
    }
    
    @Test
    public void testAsyncMap() throws Exception {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final TransformCornerCases tt = new TransformCornerCases();
        
        final CompletableFuture<Long> future = new CompletableFuture<>();
        ex.execute(AsyncTrace.wrap((Runnable) () -> async(tt.simple()).whenComplete((r, t) -> {
            if (t == null) {
                future.complete(r);
            } else {
                future.completeExceptionally(AsyncTrace.join(t));
            }
        })));
        future.get(1000, TimeUnit.SECONDS);
    }
    
    @Test
    public void testAwaitResult() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases();
        assertThat(block(tt.awaitingResult(1))).isEqualTo(1);
        assertThat(block(tt.awaitingResult(3))).isEqualTo(-1);
    }
    
    @Test
    public void testLambdaAwait() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases();
        assertThat(block(tt.lambdaAwait())).isNull();
    }
    
    @Test
    public void testStaticLambdaAwait() throws InterruptedException {
        assertThat(block(TransformCornerCases.staticLambdaAwait())).isNull();
    }
    
    @Test
    public void testGenericLambdaAwait() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases();
        assertThat(block(tt.genericLambdaAwait())).isNull();
    }
    
    @Test
    public void testStaticGenericLambdaAwait() throws InterruptedException {
        assertThat(block(TransformCornerCases.staticGenericLambdaAwait())).isNull();
    }
    
    @Test
    public void testHandleException() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases();
        assertThat(block(tt.handleException())).isNull();
    }
    
    @Test
    public void testStaticHandleException() throws InterruptedException {
        assertThat(block(TransformCornerCases.staticHandleException())).isNull();
    }
    
    @Test
    public void testThrowException() {
        final TransformCornerCases tt = new TransformCornerCases();
        assertThat(catchThrowable(() -> block(tt.throwException()))).isInstanceOf(IllegalStateException.class);
    }
    
    @Test
    public void testStaticThrowException() {
        assertThat(catchThrowable(() -> block(TransformCornerCases.staticThrowException()))).isInstanceOf(IllegalStateException.class);
    }
    
    @Test
    public void testMethodReference() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases();
        assertThat(block(tt.usingMethodReference())).isEqualTo(Long.valueOf(1L));
    }
    
    @Test
    public void testStaticMethodReference() throws InterruptedException {
        assertThat(block(TransformCornerCases.staticUsingMethodReference())).isEqualTo(Long.valueOf(1L));
    }
    
}
