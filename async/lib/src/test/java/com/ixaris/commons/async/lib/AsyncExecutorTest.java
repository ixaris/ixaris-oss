package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncExecutor.exec;
import static com.ixaris.commons.async.lib.AsyncExecutor.execAndRelay;
import static com.ixaris.commons.async.lib.AsyncExecutor.relay;
import static com.ixaris.commons.async.lib.AsyncExecutor.yield;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.async.lib.CompletionStageUtil.join;

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
        
        final Async<Void> relay = relay(exec(ex2, () -> {
            // go to ex2
            Thread.sleep(100L);
            Assertions.assertThat(AsyncExecutor.get()).isEqualTo(ex2);
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
    public void testRelayAsync() throws InterruptedException {
        final AtomicInteger threadsCreated = new AtomicInteger();
        final Executor ex1 = new AsyncExecutorWrapper<>(command -> {
            threadsCreated.incrementAndGet();
            new Thread(command).start();
        });
        final Executor ex2 = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        
        block(exec(ex1, () -> relayAsyncProcess(ex1, ex2)));
        
        Assertions.assertThat(threadsCreated.get()).isEqualTo(2);
    }
    
    private Async<Void> relayAsyncProcess(final Executor ex1, final Executor ex2) throws InterruptedException {
        final Thread startThread = Thread.currentThread();
        // start on ex1
        Assertions.assertThat(AsyncExecutor.get()).isEqualTo(ex1);
        
        final Async<Void> relay = execAndRelay(ex2, () -> {
            // go to ex2
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
            Assertions.assertThat(AsyncExecutor.get()).isEqualTo(ex2);
            return exec((CompletionStageCallableThrows<Void, RuntimeException>) Async::result);
        });
        
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
        
        block(exec(ex, AsyncExecutorTest::yielding));
        
        Assertions.assertThat(threadsCreated.get()).isEqualTo(2);
    }
    
    private static Async<Void> yielding() {
        final Thread startThread = Thread.currentThread();
        
        await(yield());
        
        // on another thread, since the yield will have caused this part of the
        // execution to be queued
        Assertions.assertThat(Thread.currentThread()).isNotEqualTo(startThread);
        
        return result();
    }
    
    @Test
    public void testRejectJoin() {
        final Executor ex = new AsyncExecutorWrapper<>(false, Executors.newFixedThreadPool(1));
        Assertions.assertThatThrownBy(() -> block(exec(ex, this::joinProcess))).isInstanceOf(UnsupportedOperationException.class);
    }
    
    private Async<Void> joinProcess() {
        join(asyncProcess());
        return result();
    }
    
    private Async<Void> asyncProcess() {
        return result();
    }
    
}
