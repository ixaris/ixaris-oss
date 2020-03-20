package com.ixaris.commons.microservices.lib.common;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;

public class AbstractServiceHeaderBuilder<C extends MessageLite, B extends AbstractServiceHeaderBuilder<C, B>> {
    
    protected final long intentId;
    protected final String tenantId;
    protected final C context;
    
    protected String sourceServiceName = null;
    protected String sourceServiceKey = null;
    protected long correlationId;
    protected long callRef = 0;
    protected long parentRef = 0;
    protected String targetServiceKey;
    
    /**
     * used to retry / resume an operation with an existing intent id
     */
    AbstractServiceHeaderBuilder(final long intentId, final String tenantId, final C context) {
        this.intentId = intentId;
        this.tenantId = tenantId;
        this.context = context;
        correlationId = UniqueIdGenerator.generate();
    }
    
    AbstractServiceHeaderBuilder(final String tenantId, final C context) {
        this.tenantId = tenantId;
        this.context = context;
        intentId = UniqueIdGenerator.generate();
        correlationId = intentId;
    }
    
    AbstractServiceHeaderBuilder(final C context) {
        this(TENANT.get(), context);
    }
    
    AbstractServiceHeaderBuilder(final ServiceHeader<C> header) {
        this(header, header.getContext());
    }
    
    AbstractServiceHeaderBuilder(final ServiceHeader<C> header, final C context) {
        this.intentId = header.getIntentId();
        this.correlationId = header.getCorrelationId();
        this.callRef = header.getCallRef();
        this.parentRef = header.getParentRef();
        this.sourceServiceName = header.getServiceName();
        this.sourceServiceKey = header.getServiceKey();
        this.targetServiceKey = null; // reset the service key so that header can be reused
        this.tenantId = header.getTenantId();
        this.context = context;
    }
    
    @SuppressWarnings("unchecked")
    public final B withTargetServiceKey(final String targetServiceKey) {
        this.targetServiceKey = targetServiceKey;
        return (B) this;
    }
    
}
