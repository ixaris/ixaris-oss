package com.ixaris.commons.microservices.lib.common;

import java.util.Objects;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

public final class PendingKey {
    
    public static PendingKey from(final RequestEnvelope envelope) {
        return new PendingKey(envelope.getCorrelationId(), envelope.getCallRef());
    }
    
    public static PendingKey from(final ResponseEnvelope envelope) {
        return new PendingKey(envelope.getCorrelationId(), envelope.getCallRef());
    }
    
    public static PendingKey from(final EventEnvelope envelope) {
        return new PendingKey(envelope.getCorrelationId(), envelope.getCallRef());
    }
    
    public static PendingKey from(final EventAckEnvelope envelope) {
        return new PendingKey(envelope.getCorrelationId(), envelope.getCallRef());
    }
    
    private final long correlationId;
    private final long callRef;
    
    public PendingKey(final long correlationId, final long callRef) {
        this.correlationId = correlationId;
        this.callRef = callRef;
    }
    
    public long getCorrelationId() {
        return correlationId;
    }
    
    public long getCallRef() {
        return callRef;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> (correlationId == other.correlationId) && (callRef == other.callRef));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(correlationId, callRef);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("correlationId", correlationId).with("callRef", callRef).toString();
    }
    
}
