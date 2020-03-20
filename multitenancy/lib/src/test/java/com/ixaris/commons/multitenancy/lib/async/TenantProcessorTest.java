package com.ixaris.commons.multitenancy.lib.async;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.TestTenants.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.ixaris.commons.clustering.lib.idempotency.StoredPendingMessage;
import com.ixaris.commons.multitenancy.lib.async.DummyAtLeastOnceMessageType;
import com.ixaris.commons.multitenancy.lib.async.ExecutorMultiTenantAtLeastOnceProcessor;
import com.ixaris.commons.multitenancy.lib.async.ExecutorMultiTenantAtLeastOnceProcessorFactory;

public final class TenantProcessorTest {
    
    public static final String MESSAGE_SUB_TYPE = "SOME_EVENT";
    
    @Test
    public void successfulStoreAndProcess_shouldProcessAndAckSuccessfully() {
        final DummyAtLeastOnceMessageType messageType = new DummyAtLeastOnceMessageType("Events");
        
        final DummyAtLeastOnceMessageType processor = spy(messageType);
        
        final ExecutorMultiTenantAtLeastOnceProcessor<?> p = new ExecutorMultiTenantAtLeastOnceProcessorFactory(1)
            .create(processor, 200);
        p.registerTenant(DEFAULT);
        
        final StoredPendingMessage<Void> msg1 = messageType.store(MESSAGE_SUB_TYPE, null);
        final StoredPendingMessage<Void> msg2 = messageType.store(MESSAGE_SUB_TYPE, null);
        
        verify(processor, timeout(1000).times(1)).processMessage(eq(msg1), any());
        verify(processor, timeout(1000).times(1)).processMessage(eq(msg2), any());
        
        assertThat(messageType.getProcessedMessages()).containsExactly(msg1, msg2);
    }
    
    @Test
    public void successfulStoreAndProcess_StoreSomeEventsAfterQuietPeriod_shouldProcessSuccessfully() throws InterruptedException {
        final DummyAtLeastOnceMessageType messageType = new DummyAtLeastOnceMessageType("Events");
        
        final DummyAtLeastOnceMessageType processor = spy(messageType);
        
        final int refreshInterval = 200;
        final ExecutorMultiTenantAtLeastOnceProcessor<?> p = new ExecutorMultiTenantAtLeastOnceProcessorFactory(1)
            .create(processor, refreshInterval);
        p.registerTenant(DEFAULT);
        
        final StoredPendingMessage<Void> msg1 = messageType.store(MESSAGE_SUB_TYPE, null);
        final StoredPendingMessage<Void> msg2 = messageType.store(MESSAGE_SUB_TYPE, null);
        
        verify(processor, timeout(1000).times(1)).processMessage(eq(msg1), any());
        verify(processor, timeout(1000).times(1)).processMessage(eq(msg2), any());
        
        // Give some time for empty "polls"
        Thread.sleep(refreshInterval * 2);
        
        // Store a new message after quiet period
        final StoredPendingMessage<Void> msg3 = messageType.store(MESSAGE_SUB_TYPE, null);
        
        // Message should be processed
        verify(processor, timeout(1000).times(1)).processMessage(eq(msg3), any());
        
        assertThat(messageType.getProcessedMessages()).containsExactly(msg1, msg2, msg3);
    }
    
    @Test
    public void testRetry() {
        final DummyAtLeastOnceMessageType repository = new DummyAtLeastOnceMessageType("Events", aLong -> aLong == 1);
        
        final DummyAtLeastOnceMessageType processor = spy(repository);
        
        final ExecutorMultiTenantAtLeastOnceProcessor<?> p = new ExecutorMultiTenantAtLeastOnceProcessorFactory(1)
            .create(processor, 200);
        p.registerTenant(DEFAULT);
        
        final StoredPendingMessage<Void> msg1 = repository.store(MESSAGE_SUB_TYPE, null);
        final StoredPendingMessage<Void> msg2 = repository.store(MESSAGE_SUB_TYPE, null);
        
        verify(processor, timeout(5000).times(2)).processMessage(eq(msg1), any());
        verify(processor, timeout(1000).times(1)).processMessage(eq(msg2), any());
        
        assertThat(repository.getProcessedMessages()).containsExactly(msg1, msg2);
    }
    
    @Test
    public void testParallelism() {
        final DummyAtLeastOnceMessageType repository = new DummyAtLeastOnceMessageType("Events", count -> false);
        
        final DummyAtLeastOnceMessageType processor = spy(repository);
        
        final ExecutorMultiTenantAtLeastOnceProcessor<?> p = new ExecutorMultiTenantAtLeastOnceProcessorFactory(1)
            .create(processor, 200);
        p.registerTenant(DEFAULT);
        
        final int numberOfMessages = 10;
        final List<StoredPendingMessage<Void>> storedMessages = new ArrayList<>(numberOfMessages);
        for (int i = 0; i < numberOfMessages; i++) {
            storedMessages.add(repository.store(MESSAGE_SUB_TYPE, null));
        }
        
        for (int i = 0; i < numberOfMessages; i++) {
            verify(processor, timeout(1000).times(1)).processMessage(eq(storedMessages.get(i)), any());
        }
        
        assertThat(repository.getProcessedMessages()).containsExactlyElementsOf(storedMessages);
    }
    
    @Test
    public void messagesShouldBeProcessedInTheRightSequence() {
        final DummyAtLeastOnceMessageType repository = new DummyAtLeastOnceMessageType("Events");
        
        final DummyAtLeastOnceMessageType processor = spy(repository);
        
        final ExecutorMultiTenantAtLeastOnceProcessor<?> p = new ExecutorMultiTenantAtLeastOnceProcessorFactory(1)
            .create(processor, 200);
        p.registerTenant(DEFAULT);
        
        final List<StoredPendingMessage<Void>> msgs = IntStream
            .range(0, 5)
            .mapToObj(i -> repository.store(MESSAGE_SUB_TYPE, null))
            .collect(Collectors.toList());
        
        verify(processor, timeout(1000).times(5)).processMessage(any(), any());
        
        assertThat(processor.getProcessedMessages()).isEqualTo(msgs);
    }
    
    @Test
    public void messagesShouldBeProcessedInTheRightSequence_evenWithFailures() {
        final DummyAtLeastOnceMessageType repository = new DummyAtLeastOnceMessageType(
            "Events", count -> count % 2 == 0);
        
        final DummyAtLeastOnceMessageType processor = spy(repository);
        
        final ExecutorMultiTenantAtLeastOnceProcessor<?> p = new ExecutorMultiTenantAtLeastOnceProcessorFactory(1)
            .create(processor, 200);
        p.registerTenant(DEFAULT);
        
        final List<StoredPendingMessage<Void>> msgs = IntStream
            .range(0, 5)
            .mapToObj(i -> repository.store(MESSAGE_SUB_TYPE, null))
            .collect(Collectors.toList());
        
        verify(processor, timeout(5000).times(9)).processMessage(any(), any());
        
        assertThat(repository.getProcessedMessages()).isEqualTo(msgs);
    }
    
    @SuppressWarnings("squid:S2925")
    @Test
    public void successfulStoreAndProcess_pollNow_shouldProcessAndAckSuccessfully() throws InterruptedException {
        final DummyAtLeastOnceMessageType messageType = new DummyAtLeastOnceMessageType("Events");
        
        final DummyAtLeastOnceMessageType processor = spy(messageType);
        
        final ExecutorMultiTenantAtLeastOnceProcessor<?> p = new ExecutorMultiTenantAtLeastOnceProcessorFactory(1)
            .create(processor, 2000000);
        p.registerTenant(DEFAULT);
        
        Thread.sleep(500L);
        
        final StoredPendingMessage<Void> msg1 = messageType.store(MESSAGE_SUB_TYPE, null);
        TENANT.exec(DEFAULT, p::pollNow);
        
        verify(processor, timeout(20).times(1)).processMessage(eq(msg1), any());
        
        assertThat(messageType.getProcessedMessages()).containsExactly(msg1);
    }
    
}
