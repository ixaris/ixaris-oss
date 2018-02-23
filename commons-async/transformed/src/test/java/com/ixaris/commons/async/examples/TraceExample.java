package com.ixaris.commons.async.examples;

import static com.ixaris.commons.async.lib.Async.async;
import static com.ixaris.commons.async.lib.Async.block;
import static com.ixaris.commons.async.lib.AsyncExecutor.exec;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;

public class TraceExample {

    @Test
    public void testTrace() throws InterruptedException {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(5));

        try {
            block(exec(ex, () -> execute(ex, 8)));
        } catch (final IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private final Async<Void> execute(final Executor ex, final int depth) {
        if (depth == 0) {
            throw new IllegalStateException();
        } else {
            return exec(ex, () -> execute(ex, depth - 1));
        }
    }

}
