package com.ixaris.commons.async.transformed

import com.ixaris.commons.async.lib.Async
import com.ixaris.commons.async.lib.AsyncCallableThrows
import com.ixaris.commons.async.lib.AsyncExecutor
import com.ixaris.commons.async.lib.AsyncQueue
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper
import com.ixaris.commons.misc.lib.function.RunnableThrows
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
            r.add(AsyncExecutor.exec(ex, { criticalSection(s) } as AsyncCallableThrows<Void, RuntimeException>))
        }
        block allSame(r)
        
        assertThat s.counter isNotEqualTo 5
    }
    
    @Test
    void testCriticalSectionQueueing() throws InterruptedException {
        final Shared s = new Shared()
        final AsyncQueue q = new AsyncQueue()
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(5))
        final List<Async<Void>> r = new LinkedList<>()
        for (int i = 0; i < 5; i++) {
            r.add(AsyncExecutor.exec(ex, { criticalSection q, s } as AsyncCallableThrows<Void, RuntimeException>))
        }
        block allSame(r)
        
        assertThat s.counter isEqualTo 5
    }
    
    private static Async<Void> criticalSection(final AsyncQueue queue, final Shared shared) {
        return queue.exec { criticalSection(shared) }
    }
    
    private static Async<Void> criticalSection(final Shared shared) {
        final int counter = shared.counter
        System.out.println(Thread.currentThread().getName() + " start from " + counter)
        // simulate work being done
        return AsyncExecutor.schedule(10, TimeUnit.MILLISECONDS, {
            System.out.println(Thread.currentThread().getName() + " continue from " + counter)
            shared.counter = counter + 1
        } as RunnableThrows<RuntimeException>)
    }
    
}
