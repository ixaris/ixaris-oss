package com.ixaris.commons.async.transformed;

import static com.ixaris.commons.async.lib.Async.async;
import static com.ixaris.commons.async.lib.Async.block;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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
        System.out.println(future.get(1000, TimeUnit.SECONDS));
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
        System.out.println(future.get(1000, TimeUnit.SECONDS));
        
    }
    
    @Test
    public void testAwaitResult() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases();
        assertEquals(1, block(tt.awaitingResult(1)).intValue());
        assertEquals(-1, block(tt.awaitingResult(3)).intValue());
    }
    
    @Test
    public void testLambdaAwait() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases();
        assertNull(block(tt.lambdaAwait()));
        assertNull(block(TransformCornerCases.staticLambdaAwait()));
    }
    
    @Test
    public void testGenericLambdaAwait() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases();
        assertNull(block(tt.genericLambdaAwait()));
        assertNull(block(TransformCornerCases.staticGenericLambdaAwait()));
    }
    
    @Test
    public void testHandleException() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases();
        assertNull(block(tt.handleException()));
        assertNull(block(TransformCornerCases.staticHandleException()));
    }
    
    @Test
    public void testThrowException() throws InterruptedException {
        final TransformCornerCases tt = new TransformCornerCases();
        try {
            block(tt.throwException());
            fail();
        } catch (final IllegalStateException expected) {}
        try {
            block(TransformCornerCases.staticThrowException());
            fail();
        } catch (final IllegalStateException expected) {}
    }
    
}
