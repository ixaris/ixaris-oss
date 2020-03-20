package com.ixaris.commons.clustering.lib.idempotency;

import java.util.Objects;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

/**
 * POJO that represents a stored pending message that is yet to be processed with the AtLeastOnceMessageType
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class StoredPendingMessage<T> {
    
    private final long sequenceNumber;
    private final int shard;
    private final String messageSubType;
    private final T message;
    
    public StoredPendingMessage(final long sequenceNumber, final int shard, final String messageSubType, final T message) {
        this.sequenceNumber = sequenceNumber;
        this.shard = shard;
        this.messageSubType = messageSubType;
        this.message = message;
    }
    
    public long getSequenceNumber() {
        return sequenceNumber;
    }
    
    public int getShard() {
        return shard;
    }
    
    public String getMessageSubType() {
        return messageSubType;
    }
    
    public T getMessage() {
        return message;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> sequenceNumber == other.sequenceNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sequenceNumber);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this)
            .with("sequenceNumber", sequenceNumber)
            .with("shard", shard)
            .with("messageSubType", messageSubType)
            .with("message", message)
            .toString();
    }
}
