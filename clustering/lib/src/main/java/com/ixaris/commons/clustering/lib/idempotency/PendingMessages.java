package com.ixaris.commons.clustering.lib.idempotency;

import java.util.List;

public final class PendingMessages<T> {
    
    private final List<StoredPendingMessage<T>> messages;
    private final boolean morePending;
    
    public PendingMessages(final List<StoredPendingMessage<T>> messages, final boolean morePending) {
        this.messages = messages;
        this.morePending = morePending;
    }
    
    public List<StoredPendingMessage<T>> getMessages() {
        return messages;
    }
    
    public boolean isMorePending() {
        return morePending;
    }
    
}
