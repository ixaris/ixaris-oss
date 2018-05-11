package com.ixaris.commons.async.transformed;

import static com.ixaris.commons.async.lib.Async.allSame;
import static com.ixaris.commons.async.lib.AsyncExecutor.scheduleSync;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.AsyncQueue;
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;

public class CriticalSectionTest {
    
    private class Shared {
        
        private int counter = 0;
        
    }
    
    @Test
    public void testCriticalSectionNoQueueing() throws InterruptedException {
        final Shared s = new Shared();
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(5));
        
        final List<Async<Void>> r = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            r.add(AsyncExecutor.exec(ex, () -> criticalSection(s)));
        }
        block(allSame(r));
        
        assertThat(s.counter).isNotEqualTo(5);
    }
    
    @Test
    public void testCriticalSectionQueueing() throws InterruptedException {
        final Shared s = new Shared();
        final AsyncQueue q = new AsyncQueue();
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(5));
        final List<Async<Void>> r = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            r.add(AsyncExecutor.exec(ex, () -> criticalSection(q, s)));
        }
        block(allSame(r));
        
        assertThat(s.counter).isEqualTo(5);
    }
    
    private static Async<Void> criticalSection(final AsyncQueue queue, final Shared shared) {
        return queue.exec(() -> criticalSection(shared));
    }
    
    private static Async<Void> criticalSection(final Shared shared) {
        final int counter = shared.counter;
        System.out.println(Thread.currentThread().getName() + " start from " + counter);
        // simulate work being done
        return scheduleSync(10, TimeUnit.MILLISECONDS, () -> {
            System.out.println(Thread.currentThread().getName() + " continue from " + counter);
            shared.counter = counter + 1;
            return null;
        });
    }
    
}
