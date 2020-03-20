package com.ixaris.asynctest;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.AsyncExecutor.exec;
import static com.ixaris.commons.async.lib.AsyncExecutor.execAndRelay;
import static com.ixaris.commons.async.lib.AsyncExecutor.relay;

import java.util.concurrent.Executor;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageCallableThrows;

/**
 * Intentially different package because com.ixaris.commons.async is excluded in async traces
 */
public class AsyncLoggingTestHelper {
    
    public static Async<Void> executeRecursive(final Executor ex, final int depth) {
        if (depth == 0) {
            throw new IllegalStateException();
        } else {
            return relay(exec(ex, () -> executeRecursive(ex, depth - 1)));
        }
    }
    
    public static Async<Void> throwAfterAwait(final Executor ex) {
        await(dummy(ex));
        throw new IllegalStateException("Expected");
    }
    
    public static Async<Void> throwInMap(final Executor ex) {
        return dummy(ex).map(r -> {
            throw new IllegalStateException("Expected");
        });
    }
    
    public static Async<Void> throwInLoop(final Executor ex) {
        for (int i = 0; i < 100; i++) {
            await(dummy(ex));
            await(dummy(ex));
        }
        throw new IllegalStateException("Expected");
    }
    
    public static Async<Void> dummy(final Executor ex) {
        return execAndRelay(ex, (CompletionStageCallableThrows<Void, RuntimeException>) Async::result);
    }
    
}
