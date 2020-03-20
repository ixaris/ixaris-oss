package com.ixaris.commons.async.transformed;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncExecutor.execAndRelay;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;

public class TransformLoopTest {
    
    public static final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
    
    @Test
    public void testLoop() throws InterruptedException {
        assertThat(block(loop())).isEqualTo(61);
    }
    
    private Async<Integer> loop() {
        int i = 0;
        try {
            for (; i < 100; i++) {
                final int ii = i;
                await(execAndRelay(ex, () -> fork(ii)));
            }
        } catch (final RuntimeException e) {}
        return result(i);
    }
    
    private Async<Void> fork(int i) {
        if (i > 60) {
            throw new IllegalStateException();
        }
        return result();
    }
    
}
