package com.ixaris.commons.async.lib;

import com.ixaris.commons.async.test.CompletionStageAssert;
import org.junit.jupiter.api.Test;

public class AsyncTest {
    
    @Test
    public void testRejected() {
        CompletionStageAssert
            .assertThat(Async.rejected(new RuntimeException("rejected")))
            .await()
            .isRejectedWith(RuntimeException.class)
            .hasMessage("rejected");
    }
    
    @Test
    public void testFrom() {
        CompletionStageAssert
            .assertThat(Async.from(() -> {
                throw new RuntimeException("rejected");
            }))
            .await()
            .isRejectedWith(RuntimeException.class)
            .hasMessage("rejected");
    }
    
}
