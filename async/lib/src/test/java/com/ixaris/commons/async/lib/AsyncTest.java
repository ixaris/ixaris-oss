package com.ixaris.commons.async.lib;

import org.junit.jupiter.api.Test;

import com.ixaris.commons.async.test.CompletionStageAssert;

public class AsyncTest {
    
    @Test
    public void testRejected() {
        CompletionStageAssert.assertThat(Async.rejected(new RuntimeException("rejected")))
            .await()
            .isRejectedWith(RuntimeException.class)
            .hasMessage("rejected");
    }
    
    @Test
    public void testFrom() {
        CompletionStageAssert.assertThat(Async.from(() -> {
            throw new RuntimeException("rejected");
        }))
            .await()
            .isRejectedWith(RuntimeException.class)
            .hasMessage("rejected");
    }
    
}
