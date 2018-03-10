package com.ixaris.commons.async.transformed;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.Async.allSame;
import static com.ixaris.commons.async.lib.Async.async;
import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.awaitResult;
import static com.ixaris.commons.async.lib.Async.result;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.object.Tuple4;

public class TransformCornerCases {
    
    public static final class Exception1 extends Exception {
        
    }
    
    public interface Example {
        
        Async<Void> entryPoint() throws Exception1;
        
    }
    
    public static class ExampleImpl implements Example {
        
        @Override
        public Async<Void> entryPoint() {
            try {
                
                try {
                    System.out.println("Hello async world");
                    System.out.println(await(tmp()));
                    return result();
                } catch (Throwable t) { // NOSONAR future handling
                    return null;
                }
            } catch (Throwable t) { // NOSONAR future handling
                return result();
            }
        }
        
        private Async<Long> tmp() {
            return result(1L);
        }
        
        private void test() {
            async(tmp());
        }
        
    }
    
    public static final class TestHolder {
        
        private final int val;
        
        public TestHolder(final int val) {
            this.val = val;
        }
        
    }
    
    @FunctionalInterface
    public interface Callback {
        
        Async<Long> call();
        
    }
    
    // tests using a lambda that returns Async
    public Async<Long> usingLambdaWithBody() {
        final List<Async<Integer>> stages = Arrays.stream(new Integer[] { 1, 2 })
            .map(i -> async(lambdaWithBodyHelper(i)))
            .collect(Collectors.toList());
        return allSame(stages).map(ok -> ok.stream().reduce(0, (a, b) -> a + b)).map(a -> (long) a);
    }
    
    public CompletionStage<Integer> lambdaWithBodyHelper(final int i) {
        return CompletableFuture.completedFuture(i);
    }
    
    // test method reference
    public Async<Long> usingMethodReference() {
        return usingMethodReferenceHelper(this::methodToReference);
    }
    
    public Async<Long> usingMethodReferenceHelper(final Callback callback) {
        return callback.call();
    }
    
    public Async<Long> methodToReference() {
        return result(1L);
    }
    
    public Async<Long> simple() {
        return operation3().map(x -> x * 2);
    }
    
    public Async<Long> operation() {
        try {
            final Async<Integer> op2 = operation2(0);
            final TestHolder x = new TestHolder(await(op2));
            final Tuple4<Integer, Long, Long, Long> c = await(all(operation2(0), usingLambdaWithBody(), usingMethodReference(), async(futureOperation())));
            long result = x.val + (c.get1() * 2 + c.get2() + c.get3() + c.get4());
            System.out.println(this + "1: " + result);
            
            for (int i = 1; i <= 3; i++) {
                try {
                    result += await(operation2(i));
                    System.out.println(this + "2: " + i + " " + result);
                } catch (final Exception1 e) {
                    result += 10;
                    System.out.println(this + "3: " + i + " " + result);
                }
            }
            
            System.out.println(this + "4: " + result);
            return result(result);
        } catch (final Exception1 e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static Async<Integer> operation2(final int i) throws Exception1 {
        return AsyncExecutor.exec(() -> {
            System.out.println("OP2 " + i);
            if (i == 3) {
                throw new Exception1();
            }
            return result(i);
        });
    }
    
    public Async<Long> operation3() {
        return AsyncExecutor.execSync(() -> 1L);
    }
    
    public CompletionStage<Long> futureOperation() {
        return async(AsyncExecutor.execSync(() -> 3L));
    }
    
    public Async<Integer> awaitingResult(final int i) {
        try {
            return awaitResult(operation2(i));
        } catch (final Exception1 e) {
            return result(-1);
        }
    }
    
    public Async<Void> lambdaAwait() {
        return processLambda(() -> {
            final long result = await(AsyncExecutor.execSync(() -> 0L));
            return result(result);
        });
    }
    
    private Async<Void> processLambda(final Callback callback) {
        return callback.call().map(r -> null);
    }
    
    public static Async<Void> staticLambdaAwait() {
        return staticProcessLambda(() -> {
            final long result = await(AsyncExecutor.execSync(() -> 0L));
            return result(result);
        });
    }
    
    private static Async<Void> staticProcessLambda(final Callback callback) {
        return callback.call().map(r -> null);
    }
    
    public Async<Void> genericLambdaAwait() {
        return processGenericLambda(() -> {
            await(AsyncExecutor.execSync(() -> 0L));
            return result();
        });
    }
    
    private <T, E extends Exception> T processGenericLambda(final CallableThrows<T, E> callable) throws E {
        return callable.call();
    }
    
    public static Async<Void> staticGenericLambdaAwait() {
        return staticProcessGenericLambda(() -> {
            await(AsyncExecutor.execSync(() -> 0L));
            return result();
        });
    }
    
    private static <T, E extends Exception> T staticProcessGenericLambda(final CallableThrows<T, E> callable) throws E {
        return callable.call();
    }
    
    public Async<Void> handleException() {
        try {
            throw new IllegalStateException();
        } catch (final IllegalStateException e) {
            return result();
        }
    }
    
    public static Async<Void> staticHandleException() {
        try {
            throw new IllegalStateException();
        } catch (final IllegalStateException e) {
            return result();
        }
    }
    
    public Async<Void> throwException() {
        throw new IllegalStateException();
    }
    
    public static Async<Void> staticThrowException() {
        throw new IllegalStateException();
    }

    public static Async<Void> infiniteLoop() {
        for(;;) {
            await(result());
        }
    }
    
}
