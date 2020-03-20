package com.ixaris.commons.multitenancy.lib.async;

import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.idempotency.AtLeastOnceMessageType;
import com.ixaris.commons.clustering.lib.idempotency.PendingMessages;
import com.ixaris.commons.clustering.lib.idempotency.StoredPendingMessage;

public class DataUnitAtLeastOnceMessageTypeWrapper<T> implements AtLeastOnceMessageType<T> {
    
    private final AtLeastOnceMessageType<T> wrapped;
    private final String unit = DATA_UNIT.get();
    
    public DataUnitAtLeastOnceMessageTypeWrapper(final AtLeastOnceMessageType<T> wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public String getKey() {
        return wrapped.getKey() + unit;
    }
    
    @Override
    public boolean isFailedMessageBlocksQueue() {
        return wrapped.isFailedMessageBlocksQueue();
    }
    
    @Override
    public Async<PendingMessages<T>> pending(final long timestamp) {
        return DATA_UNIT.exec(unit, () -> wrapped.pending(timestamp));
    }
    
    @Override
    public Async<Void> processMessage(final StoredPendingMessage<T> pendingMessage,
                                      final NextRetryTimeFunction nextRetryTimeFunction) {
        return DATA_UNIT.exec(unit, () -> wrapped.processMessage(pendingMessage, nextRetryTimeFunction));
    }
    
}
