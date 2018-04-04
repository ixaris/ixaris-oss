package com.ixaris.commons.async.transformed

import com.ixaris.commons.async.lib.AsyncTrace
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
        final Runnable runnable = {
            tt.async$operation().whenComplete({ r, t ->
                if (t == null) {
                    future.complete(r)
                } else {
                    future.completeExceptionally(AsyncTrace.join(t))
                }
            })
        }
        ex.execute(AsyncTrace.wrap(runnable))
        future.get(1000, TimeUnit.SECONDS)
    }

    @Test
    void testAsyncMap() throws Exception {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1))
        final TransformCornerCases tt = new TransformCornerCases()

        final CompletableFuture<Long> future = new CompletableFuture<>()
        final Runnable runnable = {
            tt.async$simple().whenComplete({ r, t ->
                if (t == null) {
                    future.complete(r)
                } else {
                    future.completeExceptionally(AsyncTrace.join(t))
                }
            })
        }
        ex.execute(AsyncTrace.wrap(runnable))
        future.get(1000, TimeUnit.SECONDS)
    }

    @Test
    void testAwaitResult() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat block(tt.async$awaitingResult(1)) isEqualTo 1
        assertThat block(tt.async$awaitingResult(3)) isEqualTo -1
    }

    @Test
    void testLambdaAwait() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat block(tt.async$lambdaAwait()) isNull()
    }

    @Test
    void testStaticLambdaAwait() throws InterruptedException {
        assertThat block(TransformCornerCases.async$staticLambdaAwait()) isNull()
    }

    @Test
    void testGenericLambdaAwait() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat block(tt.async$genericLambdaAwait()) isNull()
    }

    @Test
    void testStaticGenericLambdaAwait() throws InterruptedException {
        assertThat block(TransformCornerCases.async$staticGenericLambdaAwait()) isNull()
    }

    @Test
    void testHandleException() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat block(tt.async$handleException()) isNull()
    }

    @Test
    void testStaticHandleException() throws InterruptedException {
        assertThat block(TransformCornerCases.async$staticHandleException()) isNull()
    }

    @Test
    void testThrowException() {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat catchThrowable { block(tt.async$throwException()) } isInstanceOf IllegalStateException.class
    }

    @Test
    void testStaticThrowException() {
        assertThat catchThrowable { block(TransformCornerCases.async$staticThrowException()) } isInstanceOf IllegalStateException.class
    }

    @Test
    void testMethodReference() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases()
        assertThat block(tt.async$usingMethodReference()) isEqualTo Long.valueOf(1L)
    }

    @Test
    void testStaticMethodReference() throws InterruptedException {
        assertThat block(TransformCornerCases.async$staticUsingMethodReference()) isEqualTo Long.valueOf(1L)
    }
    
}
