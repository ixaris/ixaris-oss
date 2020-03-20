package com.ixaris.commons.microservices.lib.common;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

public final class ServiceEventHeader<C extends MessageLite> extends ServiceHeader<C> {
    
    /**
     * If service that is publishing the event is registered with a single key, target key is automatically set before
     * publishing. Hence, withTargetServiceKey() is only needed in case a service is registered with multiple keys ,
     * e.g. proxy/gateway.
     */
    public static final class Builder<C extends MessageLite> extends AbstractServiceHeaderBuilder<C, Builder<C>> {
        
        private Long partitionId = null;
        
        public Builder(final long intentId, final String tenantId, final C context) {
            super(intentId, tenantId, context);
        }
        
        public Builder(final String tenantId, final C context) {
            super(tenantId, context);
        }
        
        public Builder(final C context) {
            super(context);
        }
        
        private Builder(final ServiceHeader<C> header) {
            this(header, header.getContext());
        }
        
        private Builder(final ServiceHeader<C> header, final C context) {
            super(header, context);
        }
        
        public Builder<C> withPartitionId(final Long partitionId) {
            this.partitionId = partitionId;
            return this;
        }
        
        public ServiceEventHeader<C> build() {
            return new ServiceEventHeader<>(correlationId,
                callRef,
                parentRef,
                sourceServiceName,
                sourceServiceKey,
                targetServiceKey,
                intentId,
                tenantId,
                context,
                (partitionId != null) ? partitionId : ((tenantId != null) ? tenantId.hashCode() : 0L));
        }
        
    }
    
    public static <C extends MessageLite> Builder<C> newBuilder(final long intentId,
                                                                final String tenantId,
                                                                final C context) {
        return new Builder<>(intentId, tenantId, context);
    }
    
    public static <C extends MessageLite> Builder<C> newBuilder(final String tenantId, final C context) {
        return new Builder<>(tenantId, context);
    }
    
    public static <C extends MessageLite> Builder<C> newBuilder(final C context) {
        return new Builder<>(context);
    }
    
    public static <C extends MessageLite> Builder<C> newBuilder(final ServiceHeader<C> header) {
        return new Builder<>(header);
    }
    
    public static <C extends MessageLite> Builder<C> newBuilder(final ServiceHeader<C> header, final C context) {
        return new Builder<>(header, context);
    }
    
    public static <C extends MessageLite> ServiceEventHeader<C> from(final EventEnvelope eventEnvelope, final C context) {
        return new ServiceEventHeader<>(eventEnvelope.getCorrelationId(),
            eventEnvelope.getCallRef(),
            eventEnvelope.getParentRef(),
            eventEnvelope.getServiceName(),
            eventEnvelope.getServiceKey(),
            null,
            eventEnvelope.getIntentId(),
            eventEnvelope.getTenantId(),
            context,
            eventEnvelope.getPartitionId());
    }
    
    public static <C extends MessageLite> ServiceEventHeader<C> from(final ServiceHeader<C> header) {
        return newBuilder(header).build();
    }
    
    public static <C extends MessageLite> ServiceEventHeader<C> from(final String serviceKey,
                                                                     final ServiceHeader<C> header) {
        return newBuilder(header).withTargetServiceKey(serviceKey).build();
    }
    
    public static <C extends MessageLite> ServiceEventHeader<C> from(final ServiceHeader<C> header, final C context) {
        return newBuilder(header, context).build();
    }
    
    private final Long partitionId;
    
    ServiceEventHeader(final long correlationId,
                       final long callRef,
                       final long parentRef,
                       final String serviceName,
                       final String serviceKey,
                       final String targetServiceKey,
                       final long intentId,
                       final String tenantId,
                       final C context,
                       final long partitionId) {
        super(correlationId, callRef, parentRef, serviceName, serviceKey, targetServiceKey, intentId, tenantId, context);
        this.partitionId = partitionId;
    }
    
    public long getPartitionId() {
        return partitionId;
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("super", super.toString()).with("partitionId", partitionId).toString();
    }
    
}
