package com.ixaris.commons.async.transformed

import com.ixaris.commons.async.lib.Async
import com.ixaris.commons.async.lib.AsyncExecutor
import com.ixaris.commons.async.lib.CompletionStageQueue
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper
import org.junit.Test

import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static com.ixaris.commons.async.lib.CompletionStageUtil.allSame
import static com.ixaris.commons.async.lib.CompletionStageUtil.block
import static org.assertj.core.api.Assertions.assertThat

class GroovyCriticalSectionTest {
    
    private class Shared {
        
        private int counter = 0
        
    }
    
    @Test
    void testCriticalSectionNoQueueing() throws InterruptedException {
        final Shared s = new Shared()
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(5))
        
        final List<CompletionStage<Void>> r = new LinkedList<>()
        for (int i = 0; i < 5; i++) {
            r.add(AsyncExecutor.async$exec(ex, { criticalSection(s) }))
        }
        block allSame(r)
        
        assertThat s.counter isNotEqualTo 5
    }
    
    @Test
    void testCriticalSectionQueueing() throws InterruptedException {
        final Shared s = new Shared()
        final CompletionStageQueue q = new CompletionStageQueue()
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(5))
        final List<Async<Void>> r = new LinkedList<>()
        for (int i = 0; i < 5; i++) {
            r.add(AsyncExecutor.async$exec(ex, { criticalSection(q, s) }))
        }
        block allSame(r)
        
        assertThat s.counter isEqualTo 5
    }
    
    private static CompletionStage<Void> criticalSection(final CompletionStageQueue queue, final Shared shared) {
        return queue.exec { criticalSection(shared) }
    }
    
    private static CompletionStage<Void> criticalSection(final Shared shared) {
        final int counter = shared.counter
        System.out.println(Thread.currentThread().getName() + " start from " + counter)
        // simulate work being done
        return AsyncExecutor.async$scheduleSync(10, TimeUnit.MILLISECONDS, {
            System.out.println(Thread.currentThread().getName() + " continue from " + counter)
            shared.counter = counter + 1
            return null
        })
    }
    
}
