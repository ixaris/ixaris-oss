package com.ixaris.commons.microservices.lib.common;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

public final class ServiceOperationHeader<C extends MessageLite> extends ServiceHeader<C> {
    
    public static final class Builder<C extends MessageLite> extends AbstractServiceHeaderBuilder<C, Builder<C>> {
        
        private int timeout = 0;
        
        private Builder(final long intentId, final String tenantId, final C context) {
            super(intentId, tenantId, context);
        }
        
        private Builder(final String tenantId, final C context) {
            super(tenantId, context);
        }
        
        private Builder(final C context) {
            super(context);
        }
        
        private Builder(final ServiceHeader<C> header) {
            this(header, header.getContext());
        }
        
        private Builder(final ServiceHeader<C> header, final C context) {
            super(header, context);
            this.timeout = 0;
        }
        
        public Builder<C> withTimeout(final int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public ServiceOperationHeader<C> build() {
            return new ServiceOperationHeader<>(correlationId,
                callRef,
                parentRef,
                sourceServiceName,
                sourceServiceKey,
                targetServiceKey,
                intentId,
                tenantId,
                context,
                timeout);
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
    
    public static <C extends MessageLite> ServiceOperationHeader<C> from(final RequestEnvelope requestEnvelope,
                                                                         final C context) {
        return new ServiceOperationHeader<>(requestEnvelope.getCorrelationId(),
            requestEnvelope.getCallRef(),
            requestEnvelope.getParentRef(),
            requestEnvelope.getServiceName(),
            requestEnvelope.getServiceKey(),
            null,
            requestEnvelope.getIntentId(),
            requestEnvelope.getTenantId(),
            context,
            0);
    }
    
    public static <C extends MessageLite> ServiceOperationHeader<C> from(final ServiceHeader<C> header) {
        return newBuilder(header).build();
    }
    
    public static <C extends MessageLite> ServiceOperationHeader<C> from(final String serviceKey,
                                                                         final ServiceHeader<C> header) {
        return newBuilder(header).withTargetServiceKey(serviceKey).build();
    }
    
    /**
     * @deprecated use {@link #from(int, String, ServiceHeader)}m
     */
    @Deprecated
    public static <C extends MessageLite> ServiceOperationHeader<C> from(final long timeout,
                                                                         final String serviceKey,
                                                                         final ServiceHeader<C> header) {
        return newBuilder(header).withTargetServiceKey(serviceKey).withTimeout((int) timeout).build();
    }
    
    public static <C extends MessageLite> ServiceOperationHeader<C> from(final int timeout,
                                                                         final String serviceKey,
                                                                         final ServiceHeader<C> header) {
        return newBuilder(header).withTargetServiceKey(serviceKey).withTimeout(timeout).build();
    }
    
    public static <C extends MessageLite> ServiceOperationHeader<C> from(final ServiceHeader<C> header, final C context) {
        return newBuilder(header, context).build();
    }
    
    /**
     * @deprecated use {@link #from(int, String, ServiceHeader)}m
     */
    @Deprecated
    public static <C extends MessageLite> ServiceOperationHeader<C> from(final long timeout,
                                                                         final ServiceHeader<C> header,
                                                                         final C context) {
        return newBuilder(header, context).withTimeout((int) timeout).build();
    }
    
    public static <C extends MessageLite> ServiceOperationHeader<C> from(final int timeout,
                                                                         final ServiceHeader<C> header,
                                                                         final C context) {
        return newBuilder(header, context).withTimeout(timeout).build();
    }
    
    private final int timeout;
    
    @Deprecated
    public ServiceOperationHeader(final long intentId, final String tenantId, final C context) {
        this(0, intentId, tenantId, context);
    }
    
    @Deprecated
    public ServiceOperationHeader(final int timeout, final long intentId, final String tenantId, final C context) {
        this(timeout, intentId, null, tenantId, context);
    }
    
    @Deprecated
    public ServiceOperationHeader(final long intentId, final String serviceKey, final String tenantId, final C context) {
        this(0, intentId, serviceKey, tenantId, context);
    }
    
    @Deprecated
    public ServiceOperationHeader(final int timeout,
                                  final long intentId,
                                  final String serviceKey,
                                  final String tenantId,
                                  final C context) {
        this(UniqueIdGenerator.generate(), 0, 0, null, null, serviceKey, intentId, tenantId, context, timeout);
    }
    
    ServiceOperationHeader(final long correlationId,
                           final long callRef,
                           final long parentRef,
                           final String serviceName,
                           final String serviceKey,
                           final String targetServiceKey,
                           final long intentId,
                           final String tenantId,
                           final C context,
                           final int timeout) {
        super(correlationId, callRef, parentRef, serviceName, serviceKey, targetServiceKey, intentId, tenantId, context);
        this.timeout = timeout;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("super", super.toString()).with("timeout", timeout).toString();
    }
}
