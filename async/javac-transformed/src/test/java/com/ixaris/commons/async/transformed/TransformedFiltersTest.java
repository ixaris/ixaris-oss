package com.ixaris.commons.async.transformed;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.from;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;

import java.util.concurrent.CompletionStage;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageUtil;
import com.ixaris.commons.async.lib.filter.AsyncFilter;
import com.ixaris.commons.async.lib.filter.AsyncFilterChain;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;

public class TransformedFiltersTest {
    
    public interface TestFilter extends AsyncFilter<String, String> {
        
        default Async<String> onRequest(final String request) {
            return result(request);
        }
        
        default Async<String> onResponse(final String response) {
            return result(response);
        }
        
        @Override
        default Async<String> doFilter(final String origRequest, final AsyncFilterNext<String, String> next) {
            final String request = await(onRequest(origRequest));
            final String origResponse = await(next.next(request));
            return onResponse(origResponse);
        }
        
    }
    
    public interface TestFilterManualTransformation extends AsyncFilter<String, String> {
        
        default Async<String> onRequest(final String request) {
            return result(request);
        }
        
        default Async<String> onResponse(final String response) {
            return result(response);
        }
        
        @Override
        default Async<String> doFilter(final String origRequest, final AsyncFilterNext<String, String> next) {
            return from(continuation$doFilter(origRequest, next, null, (short) 0, null));
        }
        
        default CompletionStage<String> continuation$doFilter(final String origRequest,
                                                              final AsyncFilterNext<String, String> next,
                                                              String request,
                                                              final short async$index,
                                                              CompletionStage<?> async$stage) {
            switch (async$index) {
                case 0:
                    async$stage = onRequest(origRequest);
                    if (!CompletionStageUtil.isDone(async$stage)) {
                        return CompletionStageUtil.doneCompose(async$stage, f -> continuation$doFilter(origRequest, next, null, (short) 1, f));
                    }
                case 1:
                    request = (String) CompletionStageUtil.get(async$stage);
                    async$stage = next.next(request);
                    if (!CompletionStageUtil.isDone(async$stage)) {
                        final String finalRequest = request;
                        return CompletionStageUtil.doneCompose(async$stage, f -> continuation$doFilter(origRequest, next, finalRequest, (short) 2, f));
                    }
                case 2:
                    final String origResponse = (String) CompletionStageUtil.get(async$stage);
                    return onResponse(origResponse);
                default:
                    throw new IllegalArgumentException();
            }
        }
        
    }
    
    public static final class TestFilterImpl implements AsyncFilter<String, String> {
        
        private final String toAppend;
        
        public TestFilterImpl(final String toAppend) {
            this.toAppend = toAppend;
        }
        
        private Async<String> onRequest(final String request) {
            return result(toAppend + request);
        }
        
        private Async<String> onResponse(final String response) {
            return result(response + toAppend);
        }
        
        @Override
        public Async<String> doFilter(final String origRequest, final AsyncFilterNext<String, String> next) {
            final String request = await(onRequest(origRequest));
            final String origResponse = await(next.next(request));
            return onResponse(origResponse);
        }
        
    }
    
    @Test
    public void testFilterLambda() throws InterruptedException {
        
        final AsyncFilterChain<String, String> filterChain = new AsyncFilterChain<>((in, next) -> next.next("1" + in).map(s -> s + "1"));
        
        Assertions
            .assertThat(block(filterChain.exec(
                "TEST",
                s -> result("0" + s + "0"),
                (in, t) -> {
                    throw new IllegalStateException(t);
                })))
            .isEqualTo("01TEST01");
    }
    
    @Test
    public void testFilterLambdaWithAwait() throws InterruptedException {
        
        final AsyncFilterChain<String, String> filterChain = new AsyncFilterChain<>((origRequest, next) -> {
            final String request = await(result("1" + origRequest));
            final String origResponse = await(next.next(request));
            return result(origResponse + "1");
        });
        
        Assertions
            .assertThat(block(filterChain.exec(
                "TEST",
                s -> result("0" + s + "0"),
                (in, t) -> {
                    throw new IllegalStateException(t);
                })))
            .isEqualTo("01TEST01");
    }
    
    @Test
    public void testFilterImpl() throws InterruptedException {
        
        final AsyncFilterChain<String, String> filterChain = new AsyncFilterChain<>(new TestFilterImpl("1"));
        
        Assertions
            .assertThat(block(filterChain.exec(
                "TEST",
                s -> result("0" + s + "0"),
                (in, t) -> {
                    throw new IllegalStateException(t);
                })))
            .isEqualTo("01TEST01");
    }
    
    @Test
    public void testFilterAwaitInDefaultMethod() throws InterruptedException {
        
        final AsyncFilterChain<String, String> filterChain = new AsyncFilterChain<>(new TestFilter() {
            
            @Override
            public Async<String> onRequest(final String request) {
                return result("1" + request);
            }
            
            @Override
            public Async<String> onResponse(final String response) {
                return result(response + "1");
            }
            
        });
        
        Assertions
            .assertThat(block(filterChain.exec(
                "TEST",
                s -> result("0" + s + "0"),
                (in, t) -> {
                    throw new IllegalStateException(t);
                })))
            .isEqualTo("01TEST01");
    }
    
    @Test
    public void testFilterDefaultMethodManuallyTransformed() throws InterruptedException {
        
        final AsyncFilterChain<String, String> filterChain = new AsyncFilterChain<>(new TestFilterManualTransformation() {
            
            @Override
            public Async<String> onRequest(final String request) {
                return result("1" + request);
            }
            
            @Override
            public Async<String> onResponse(final String response) {
                return result(response + "1");
            }
            
        });
        
        Assertions
            .assertThat(block(filterChain.exec(
                "TEST",
                s -> result("0" + s + "0"),
                (in, t) -> {
                    throw new IllegalStateException(t);
                })))
            .isEqualTo("01TEST01");
    }
    
}
