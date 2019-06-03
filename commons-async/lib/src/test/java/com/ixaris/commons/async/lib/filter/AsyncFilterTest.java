package com.ixaris.commons.async.lib.filter;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class AsyncFilterTest {
    
    @Test
    public void testFilter() throws InterruptedException {
        
        final AsyncFilterChain<String, String> filterChain = new AsyncFilterChain<>(
            (origRequest, next) -> {
                final String request = await(result("2" + origRequest));
                final String origResponse = await(next.next(request));
                return result(origResponse + "2");
            },
            (in, next) -> next.next("1" + in).map(s -> s + "1")
        );
        
        Assertions
            .assertThat(block(filterChain.exec(
                "TEST",
                s -> result("0" + s + "0"),
                (in, t) -> {
                    throw new IllegalStateException(t);
                }
            )))
            .isEqualTo("012TEST012");
    }
    
}
