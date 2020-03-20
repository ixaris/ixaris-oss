package com.ixaris.commons.multitenancy.lib.async;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.idempotency.AtLeastOnceMessageType;
import com.ixaris.commons.clustering.lib.idempotency.PendingMessages;
import com.ixaris.commons.clustering.lib.idempotency.StoredPendingMessage;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class DummyAtLeastOnceMessageType implements AtLeastOnceMessageType<Void> {
    
    private final List<StoredPendingMessage<Void>> processedMessages = new LinkedList<>();
    
    private final List<StoredPendingMessage<Void>> messages = new LinkedList<>();
    private final AtomicLong sequenceNumber = new AtomicLong();
    private final String key;
    private final Predicate<Long> shouldFail;
    
    private long count = 0;
    
    public DummyAtLeastOnceMessageType() {
        this("TEST");
    }
    
    public DummyAtLeastOnceMessageType(final String key) {
        this(key, count -> false);
    }
    
    public DummyAtLeastOnceMessageType(final Predicate<Long> shouldFail) {
        this("TEST", shouldFail);
    }
    
    public DummyAtLeastOnceMessageType(final String key, final Predicate<Long> shouldFail) {
        this.key = key;
        this.shouldFail = shouldFail;
    }
    
    @Override
    public String getKey() {
        return key;
    }
    
    @Override
    public boolean isFailedMessageBlocksQueue() {
        return true;
    }
    
    @Override
    public Async<PendingMessages<Void>> pending(final long timestamp) {
        return result(new PendingMessages<>(new ArrayList<>(messages), false));
    }
    
    @Override
    public Async<Void> processMessage(final StoredPendingMessage<Void> pendingMessage, final NextRetryTimeFunction nextRetryTimeFunction) {
        count++;
        if (shouldFail.test(count)) {
            throw new IllegalStateException("Predicate hinted for failure");
        } else {
            processedMessages.add(pendingMessage);
            messages.remove(pendingMessage);
            return result();
        }
    }
    
    public StoredPendingMessage<Void> store(final String messageSubType, final Void processDetails) {
        final StoredPendingMessage<Void> storedPendingMessage = new StoredPendingMessage<>(
            sequenceNumber.incrementAndGet(), 0, messageSubType, processDetails);
        messages.add(storedPendingMessage);
        return storedPendingMessage;
    }
    
    public void reset() {
        processedMessages.clear();
        count = 0;
        messages.clear();
    }
    
    public List<StoredPendingMessage<Void>> getProcessedMessages() {
        return Collections.unmodifiableList(processedMessages);
    }
    
}
