package com.ixaris.commons.async.lib;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

public class AsyncQueueTest {
    
    private static final int SIZE = 100;
    
    @Test
    public void test() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(SIZE));
        final List<Integer> list = new ArrayList<>(20);
        for (int i = 0; i < SIZE; i++) {
            final int ii = i;
            ex.execute(() -> {
                try {
                    AsyncQueue.exec("TEST", 1L, () -> {
                        synchronized (list) {
                            list.add(ii);
                        }
                        Thread.sleep(10L);
                        synchronized (list) {
                            list.add(ii);
                        }
                    });
                } catch (final InterruptedException e) {
                    // this will not happen
                    throw new IllegalStateException(e);
                }
            });
        }
        
        Awaitility.await().atMost(5, SECONDS).until(() -> list.size() == SIZE * 2);
        
        for (int i = 0; i < SIZE; i++) {
            assertEquals(list.get(i * 2), list.get(i * 2 + 1));
        }
    }
    
}
