package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.block;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncExecutor.exec;
import static com.ixaris.commons.async.lib.AsyncExecutor.execSync;
import static com.ixaris.commons.async.lib.AsyncExecutor.relay;
import static com.ixaris.commons.async.lib.AsyncExecutor.yield;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;

public class AsyncExecutorTest {
    
    @Test
    public void testRelay() throws InterruptedException {
        final AtomicInteger threadsCreated = new AtomicInteger();
        final Executor ex1 = new AsyncExecutorWrapper<>(command -> {
            threadsCreated.incrementAndGet();
            new Thread(command).start();
        });
        final Executor ex2 = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        
        block(exec(ex1, () -> relayProcess(ex1, ex2)));
        
        Assertions.assertThat(threadsCreated.get()).isEqualTo(2);
    }
    
    private Async<Void> relayProcess(final Executor ex1, final Executor ex2) throws InterruptedException {
        final Thread startThread = Thread.currentThread();
        // start on ex1
        Assertions.assertThat(AsyncExecutor.get()).isEqualTo(ex1);
        
        Async<Void> relay = relay(() -> execSync(ex2, () -> {
            // go to ex2
            Thread.sleep(100L);
            Assertions.assertThat(AsyncExecutor.get()).isEqualTo(ex2);
            return null;
        }));
        
        // still on ex1 (should still be on same thread)
        Assertions.assertThat(AsyncExecutor.get()).isEqualTo(ex1);
        Assertions.assertThat(Thread.currentThread()).isEqualTo(startThread);
        
        await(relay);
        
        // back to ex1 (on another thread, since the await will have caused this part of the
        // execution to be executed after the relay)
        Assertions.assertThat(AsyncExecutor.get()).isEqualTo(ex1);
        Assertions.assertThat(Thread.currentThread()).isNotEqualTo(startThread);
        
        return result(null);
    }
    
    @Test
    public void testYield() throws InterruptedException {
        final AtomicInteger threadsCreated = new AtomicInteger();
        final Executor ex = new AsyncExecutorWrapper<>(command -> {
            threadsCreated.incrementAndGet();
            new Thread(() -> {
                try {
                    Thread.sleep(10L);
                    command.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
        
        block(exec(ex, () -> {
            final Thread startThread = Thread.currentThread();
            
            await(yield());
            
            // on another thread, since the yield will have caused this part of the
            // execution to be queued
            Assertions.assertThat(Thread.currentThread()).isNotEqualTo(startThread);
            
            return result(null);
        }));
        
        Assertions.assertThat(threadsCreated.get()).isEqualTo(2);
    }
    
}
