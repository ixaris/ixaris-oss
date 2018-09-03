package com.ixaris.commons.async.transformed;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.Test;

import com.ixaris.commons.async.lib.CompletableFutureUtil;
import com.ixaris.commons.async.lib.annotation.Async;

public class CompletionStageImplTest {
    
    public static final class FutureSubclass<T> extends CompletableFuture<T> {
        
        public static <T> FutureSubclass<T> fromCompletionStage(final CompletionStage<T> stage) {
            final FutureSubclass<T> f = new FutureSubclass<>();
            CompletableFutureUtil.complete(f, stage);
            return f;
        }
        
        public FutureSubclass() {}
        
        public FutureSubclass(final T result) {
            complete(result);
        }
        
    }
    
    @Test
    public void test() throws InterruptedException {
        block(doIt());
    }
    
    @Async
    private FutureSubclass<Void> doIt() {
        await(doOtherThing());
        return new FutureSubclass<>(null);
    }
    
    @Async
    private FutureSubclass<Void> doOtherThing() {
        return new FutureSubclass<>(null);
    }
    
}
