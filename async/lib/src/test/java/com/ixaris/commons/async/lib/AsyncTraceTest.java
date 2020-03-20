package com.ixaris.commons.async.lib;

import static com.ixaris.asynctest.AsyncLoggingTestHelper.executeRecursive;
import static com.ixaris.asynctest.AsyncLoggingTestHelper.throwAfterAwait;
import static com.ixaris.asynctest.AsyncLoggingTestHelper.throwInLoop;
import static com.ixaris.asynctest.AsyncLoggingTestHelper.throwInMap;
import static com.ixaris.commons.async.lib.AsyncExecutor.exec;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.awaitility.Awaitility;
import org.junit.Ignore;
import org.junit.Test;

import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;
import com.ixaris.commons.misc.lib.exception.StackWalker;

public class AsyncTraceTest {
    
    @Test
    public void testLoggingFromRecursive() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> c = exec(ex, () -> executeRecursive(ex, 2));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(c));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(c))
            .satisfies(e -> new ThrowableAssert(CompletionStageUtil.extractCause(e))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(cc -> new ThrowableAssert(cc.getCause())
                    .hasMessageContaining("Async Step [1] (x2) from")
                    .satisfies(c1l -> new ThrowableAssert(c1l.getCause())
                        .hasMessageContaining("Async Step [1] from")
                        .satisfies(c1 -> new ThrowableAssert(c1.getCause())
                            .hasMessageContaining("Async Step [0] from")
                            .hasNoCause()))));
    }
    
    @Test
    public void testLoggingFromAsync() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> cs = exec(ex, () -> throwAfterAwait(ex));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(cs));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(cs))
            .isInstanceOf(Throwable.class)
            .satisfies(e -> new ThrowableAssert(e).satisfies(cc -> new ThrowableAssert(cc.getCause())
                .hasMessageContaining("Async Step [1] from")
                .satisfies(c1 -> new ThrowableAssert(c1.getCause()).hasMessageContaining("Async Step [0] from").hasNoCause())));
    }
    
    @Test
    public void testLoggingFromAsyncMap() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> cs = exec(ex, () -> throwInMap(ex));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(cs));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(cs))
            .isInstanceOf(Throwable.class)
            .satisfies(e -> new ThrowableAssert(e).satisfies(c2 -> new ThrowableAssert(c2.getCause())
                .hasMessageContaining("Async Step [1] from")
                .satisfies(c1 -> new ThrowableAssert(c1.getCause()).hasMessageContaining("Async Step [0] from").hasNoCause())));
    }
    
    @Test
    public void testLoggingFromLoop() {
        final Executor ex = new AsyncExecutorWrapper<>(Executors.newFixedThreadPool(1));
        final CompletionStage<Void> cs = exec(ex, () -> throwInLoop(ex));
        
        Awaitility.await().atMost(5, SECONDS).until(() -> CompletionStageUtil.isDone(cs));
        
        Assertions
            .assertThatThrownBy(() -> CompletionStageUtil.get(cs))
            .isInstanceOf(Throwable.class)
            .satisfies(e -> new ThrowableAssert(e).satisfies(cc -> new ThrowableAssert(cc.getCause())
                .hasMessageContaining("Async Step [1] (x200) from")
                .satisfies(c1l -> new ThrowableAssert(c1l.getCause())
                    .hasMessageContaining("Async Step [1] from")
                    .satisfies(c1 -> new ThrowableAssert(c1.getCause())
                        .hasMessageContaining("Async Step [0] from")
                        .hasNoCause()))));
    }
    
    /**
     * *************************************
     */
    @Test
    @Ignore("Used to select strategy for finding caller from trace")
    public void testPerformance() {
        testMethod(new StackWalkerMethod());
        testMethod(new ThreadStackTraceMethod());
        testMethod(new ThrowableStackTraceMethod());
        
        testMethod(new StackWalkerMethod());
        testMethod(new ThreadStackTraceMethod());
        testMethod(new ThrowableStackTraceMethod());
    }
    
    private static class A {
        
        private static String getRef(final GetCallerClassNameMethod method) {
            return B.getRef(method);
        }
        
    }
    
    private static class B {
        
        private static String getRef(final GetCallerClassNameMethod method) {
            return method.getRef();
        }
        
    }
    
    /**
     * Abstract class for testing different methods of getting the caller class name
     */
    private abstract static class GetCallerClassNameMethod {
        
        public abstract String getRef();
        
        public abstract String getMethodName();
        
    }
    
    private static class StackWalkerMethod extends GetCallerClassNameMethod {
        
        public String getRef() {
            return StackWalker.findCaller(c -> !c.equals(B.class.getName()), 1);
        }
        
        public String getMethodName() {
            return "StackWalker";
        }
        
    }
    
    /**
     * Get a stack trace from the current thread
     */
    private static class ThreadStackTraceMethod extends GetCallerClassNameMethod {
        public String getRef() {
            new Throwable();
            final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            try {
                for (int index = 2;; index++) {
                    final StackTraceElement element = stackTrace[index];
                    if (!element.getClassName().equals(B.class.getName())) {
                        return element.getClassName() + "#" + element.getMethodName();
                    }
                }
            } catch (final Exception e) {
                return null;
            }
        }
        
        public String getMethodName() {
            return "Current Thread StackTrace";
        }
    }
    
    /**
     * Get a stack trace from a new Throwable
     */
    private static class ThrowableStackTraceMethod extends GetCallerClassNameMethod {
        
        public String getRef() {
            final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            try {
                for (int index = 1;; index++) {
                    final StackTraceElement element = stackTrace[index];
                    if (!element.getClassName().equals(B.class.getName())) {
                        return element.getClassName() + "#" + element.getMethodName();
                    }
                }
            } catch (final Exception e) {
                return null;
            }
        }
        
        public String getMethodName() {
            return "Throwable StackTrace";
        }
    }
    
    private static void testMethod(GetCallerClassNameMethod method) {
        long startTime = System.nanoTime();
        String ref = null;
        for (int i = 0; i < 10000; i++) {
            ref = A.getRef(method);
        }
        assertThat(ref).isEqualTo(A.class.getName() + "#getRef");
        printElapsedTime(method.getMethodName(), startTime);
    }
    
    private static void printElapsedTime(String title, long startTime) {
        System.out.println(title + ": " + ((double) (System.nanoTime() - startTime)) / 1000000 + " ms.");
    }
    
}
