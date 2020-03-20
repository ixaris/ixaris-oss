package com.ixaris.commons.microservices.test.mocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;

import org.junit.Test;

import com.ixaris.commons.async.test.CompletionStageAssert;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.test.example.TestData.Context;

public class MockEventListenerTest {
    
    @Test
    public void mockEventListener_shouldAckAndHoldReceivedEvents() {
        
        final MockEventListener<Context, Context> listener = new MockEventListener<>();
        
        final ServiceEventHeader<Context> h1 = ServiceEventHeader.newBuilder(1L, "tenant", Context.newBuilder().build()).build();
        final ServiceEventHeader<Context> h2 = ServiceEventHeader.newBuilder(2L, "tenant", Context.newBuilder().build()).build();
        
        final CompletionStage<Void> p1 = listener.onEvent(h1, Context.newBuilder().build());
        final CompletionStage<Void> p2 = listener.onEvent(h2, Context.newBuilder().build());
        final CompletionStage<Void> p3 = listener.onEvent(h2, Context.newBuilder().build());
        
        CompletionStageAssert.assertThat(p1).await().isFulfilled();
        CompletionStageAssert.assertThat(p2).await().isFulfilled();
        CompletionStageAssert.assertThat(p3).await().isFulfilled();
        assertThat(listener.get(h1).size()).isEqualTo(1);
        assertThat(listener.get(h2).size()).isEqualTo(2);
    }
}
