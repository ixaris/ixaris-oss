package com.ixaris.commons.microservices.lib.common;

import java.util.Objects;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.CommonsAsyncLib.Correlation;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

/**
 * The ServiceHeader represent general parameters that are sent with every service request or events. These fields are
 * normally intended to be used by the microservices infrastructure for routing, monitoring and multitenancy. The
 * parameter context C is a custom Protobuf message to make the ServiceHeaders extensible based on the system using the
 * microservices library. Typically, it can contain data such as Subject/credentials to be used for
 * authorisation/authentication purposes and identifying who is doing what.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 * @param <C> The protobuf object that represents the Context of the call.
 */
public class ServiceHeader<C extends MessageLite> {
    
    public static Correlation extractCorrelation(final ServiceHeader<?> header) {
        return Correlation.newBuilder().setCorrelationId(header.correlationId).setIntentId(header.intentId).build();
    }
    
    public static <C extends MessageLite> ServiceHeader<C> build(final long correlationId,
                                                                 final long callRef,
                                                                 final long parentRef,
                                                                 final String serviceName,
                                                                 final String serviceKey,
                                                                 final long intentId,
                                                                 final String tenantId,
                                                                 final C context) {
        return new ServiceHeader<>(correlationId,
            callRef,
            parentRef,
            serviceName,
            serviceKey,
            null,
            intentId,
            tenantId,
            context);
    }
    
    private final long correlationId;
    private final long callRef;
    private final long parentRef;
    private final String serviceName;
    private final String serviceKey;
    private final String targetServiceKey;
    private final long intentId;
    private final String tenantId;
    private final C context;
    
    ServiceHeader(final long correlationId,
                  final long callRef,
                  final long parentRef,
                  final String serviceName,
                  final String serviceKey,
                  final String targetServiceKey,
                  final long intentId,
                  final String tenantId,
                  final C context) {
        this.correlationId = correlationId;
        this.callRef = callRef;
        this.parentRef = parentRef;
        this.serviceName = serviceName;
        this.serviceKey = serviceKey;
        this.targetServiceKey = targetServiceKey;
        this.intentId = intentId;
        this.tenantId = tenantId;
        this.context = context;
    }
    
    public final long getCorrelationId() {
        return correlationId;
    }
    
    public final long getCallRef() {
        return callRef;
    }
    
    public final long getParentRef() {
        return parentRef;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public final String getServiceKey() {
        return serviceKey;
    }
    
    public final String getTargetServiceKey() {
        return targetServiceKey;
    }
    
    public final long getIntentId() {
        return intentId;
    }
    
    public final String getTenantId() {
        return tenantId;
    }
    
    public final C getContext() {
        return context;
    }
    
    @Override
    public final boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> (correlationId == other.correlationId) && (callRef == other.callRef));
    }
    
    @Override
    public final int hashCode() {
        return Objects.hash(correlationId, callRef);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this)
            .with("correlationId", correlationId)
            .with("callRef", callRef)
            .with("parentRef", parentRef)
            .with("serviceName", serviceName)
            .with("serviceKey", serviceKey)
            .with("targetServiceKey", targetServiceKey)
            .with("intentId", intentId)
            .with("tenantId", tenantId)
            .with("context", context)
            .toString();
    }
}
