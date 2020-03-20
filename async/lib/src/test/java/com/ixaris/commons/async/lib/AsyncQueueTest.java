package com.ixaris.commons.async.lib;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;

import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;

public class AsyncQueueTest {
    
    private static final int SIZE = 100;
    
    private static final AsyncLocal<Integer> ID = new AsyncLocal<>("ID");
    
    @Test
    @SuppressWarnings("squid:S2925")
    public void test() {
        final LongIdAsyncQueue queue = new LongIdAsyncQueue();
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(SIZE));
        final List<Integer> list = new ArrayList<>(SIZE * 2);
        final Set<Integer> iterations = new HashSet<>();
        final Set<Integer> postIterations = new HashSet<>();
        for (int i = 0; i < SIZE; i++) {
            final int ii = i;
            ex.execute(() -> ID.exec(ii, () -> queue
                .exec(1L, () -> {
                    // add async local here to test retention of the correct async local in task
                    iterations.add(ID.get());
                    synchronized (list) {
                        list.add(ID.get());
                    }
                    try {
                        Thread.sleep(20L);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    synchronized (list) {
                        list.add(ID.get());
                    }
                })
                .whenComplete((r, t) -> {
                    // add async local here to test retention of the correct async local in composed tasks
                    postIterations.add(ID.get());
                })));
        }
        
        Awaitility.await().atMost(5, SECONDS).until(() -> list.size() == SIZE * 2);
        
        for (int i = 0; i < SIZE; i++) {
            // verify unique async local value retained for each queued task
            Assert.assertTrue(iterations.remove(i));
            // verify unique async local value retained for each composed task
            Assert.assertTrue(postIterations.remove(i));
            assertEquals(list.get(i * 2), list.get(i * 2 + 1));
        }
    }
    
}
