package com.ixaris.commons.microservices.defaults.context;

import java.util.Optional;
import java.util.function.Supplier;

import com.google.protobuf.InvalidProtocolBufferException;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.CommonsAsyncLib.AsyncLocalValue;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Header;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Subject;
import com.ixaris.commons.microservices.lib.common.ServiceHeader;
import com.ixaris.commons.misc.lib.startup.StartupTask;
import com.ixaris.commons.protobuf.lib.MessageHelper;

public final class AsyncLocals implements StartupTask {
    
    public static final AsyncLocal<ServiceHeader<Context>> HEADER = new AsyncLocal<ServiceHeader<Context>>("header") {
        
        @Override
        public AsyncLocalValue encode(final ServiceHeader<Context> value) {
            return AsyncLocalValue.newBuilder().setBytesValue(Header.newBuilder()
                .setCorrelationId(value.getCorrelationId())
                .setCallRef(value.getCallRef())
                .setParentRef(value.getParentRef())
                .setServiceName(value.getServiceName())
                .setServiceKey(value.getServiceKey())
                .setIntentId(value.getIntentId())
                .setTenantId(value.getTenantId())
                .setContext(value.getContext())
                .build()
                .toByteString()).build();
        }
        
        @Override
        public ServiceHeader<Context> decode(final AsyncLocalValue value) throws InvalidProtocolBufferException {
            final Header decoded = MessageHelper.parse(Header.class, value.getBytesValue());
            return ServiceHeader.build(decoded.getCorrelationId(),
                decoded.getCallRef(),
                decoded.getParentRef(),
                decoded.getServiceName(),
                decoded.getServiceKey(),
                decoded.getIntentId(),
                decoded.getTenantId(),
                decoded.getContext());
        }
        
    };
    
    public static final Supplier<Subject> SUBJECT = () -> Optional.ofNullable(HEADER.get()).map(h -> h.getContext().getSubject()).orElseGet(Subject::getDefaultInstance);
    
    @Override
    public void run() {
        // force register async locals    
    }
    
}
