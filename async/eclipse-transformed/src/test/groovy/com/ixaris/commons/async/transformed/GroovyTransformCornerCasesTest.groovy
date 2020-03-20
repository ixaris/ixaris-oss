package com.ixaris.commons.async.transformed

import com.ixaris.commons.async.lib.AsyncTrace
import com.ixaris.commons.async.lib.CompletableFutureUtil
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper
import org.junit.Test

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static com.ixaris.commons.async.lib.CompletionStageUtil.block
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.catchThrowable

class GroovyTransformCornerCasesTest {
    
    @Test
    void testAsyncProcessCanWorkWithSingleThread() throws Exception {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1))
        final TransformCornerCases tt = new TransformCornerCases()

        final CompletableFuture<Long> future = new CompletableFuture<>()
        ex.execute(AsyncTrace.wrap({ tt.operation().whenComplete({ r, t -> CompletableFutureUtil.complete(future, r, t) }) }))
        future.get(1000, TimeUnit.SECONDS)
    }

    @Test
    void testAsyncMap() throws Exception {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1))
        final TransformCornerCases tt = new TransformCornerCases()

        final CompletableFuture<Long> future = new CompletableFuture<>()
        ex.execute(AsyncTrace.wrap({ tt.simple().whenComplete({ r, t -> CompletableFutureUtil.complete(future, r, t) }) }))
        future.get(1000, TimeUnit.SECONDS)
    }

    @Test
    void testAwaitResult() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat block(tt.awaitingResult(1)) isEqualTo 1
        assertThat block(tt.awaitingResult(3)) isEqualTo -1
    }

    @Test
    void testLambdaAwait() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat block(tt.lambdaAwait()) isNull()
    }

    @Test
    void testStaticLambdaAwait() throws InterruptedException {
        assertThat block(TransformCornerCases.staticLambdaAwait()) isNull()
    }

    @Test
    void testGenericLambdaAwait() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat block(tt.genericLambdaAwait()) isNull()
    }

    @Test
    void testStaticGenericLambdaAwait() throws InterruptedException {
        assertThat block(TransformCornerCases.staticGenericLambdaAwait()) isNull()
    }

    @Test
    void testHandleException() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat block(tt.handleException()) isNull()
    }

    @Test
    void testStaticHandleException() throws InterruptedException {
        assertThat block(TransformCornerCases.staticHandleException()) isNull()
    }

    @Test
    void testThrowException() {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat catchThrowable { block(tt.throwException()) } isInstanceOf IllegalStateException.class
    }

    @Test
    void testStaticThrowException() {
        assertThat catchThrowable { block(TransformCornerCases.staticThrowException()) } isInstanceOf IllegalStateException.class
    }

    @Test
    void testMethodReference() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat block(tt.usingMethodReference()) isEqualTo Long.valueOf(1L)
    }

    @Test
    void testStaticMethodReference() throws InterruptedException {
        assertThat block(TransformCornerCases.staticUsingMethodReference()) isEqualTo Long.valueOf(1L)
    }
    
}
