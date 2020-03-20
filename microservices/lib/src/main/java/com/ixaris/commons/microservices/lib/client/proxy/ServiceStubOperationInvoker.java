package com.ixaris.commons.microservices.lib.client.proxy;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;
import com.ixaris.commons.microservices.lib.common.exception.ServerErrorException;
import com.ixaris.commons.microservices.lib.common.exception.ServerUnavailableException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.common.tracing.TracingServiceOperationReference.TracingServiceOperationReferenceBuilder;
import com.ixaris.commons.microservices.lib.common.tracing.TracingUtil;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.protobuf.lib.MessageHelper;

final class ServiceStubOperationInvoker<T extends ServiceStub> extends UntypedOperationInvoker {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceStubOperationInvoker.class);
    
    private final ServiceStubProxy<T> proxy;
    private final int defaultTimeout;
    private final String serviceName;
    
    ServiceStubOperationInvoker(final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain,
                                final KeyAvailableCondition keyAvailableCondition,
                                final ServiceStubProxy<T> proxy,
                                final int defaultTimeout) {
        super(filterChain, keyAvailableCondition);
        
        if (proxy == null) {
            throw new IllegalArgumentException("proxy is null");
        }
        
        this.proxy = proxy;
        this.defaultTimeout = defaultTimeout;
        this.serviceName = ServiceStub.extractServiceName(proxy.serviceStubType);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + proxy.serviceStubType;
    }
    
    Async<?> invoke(final ServicePathHolder path,
                    final ServicePathHolder params,
                    final StubResourceMethodInfo<?, ?, ?> methodInfo,
                    final ServiceOperationHeader<?> header,
                    final MessageLite request) throws ClientConflictException {
        final Map<String, String> tracingHeaders = TracingUtil.getTracingHeaders(TracingServiceOperationReferenceBuilder.builder()
            .withServiceName(serviceName)
            .withServiceKey(header.getTargetServiceKey())
            .withMethod(methodInfo.getName())
            .build());
        
        // transform header and request in envelope
        final RequestEnvelope.Builder builder = RequestEnvelope.newBuilder()
            .setCorrelationId(header.getCorrelationId())
            .setCallRef(UniqueIdGenerator.generate())
            .setParentRef(header.getCallRef())
            .setServiceName(serviceName)
            .addAllPath(path)
            .addAllParams(params)
            .setMethod(methodInfo.getName())
            .setIntentId(header.getIntentId())
            .setContext(header.getContext().toByteString())
            .setTimeout(header.getTimeout() > 0 ? header.getTimeout() : defaultTimeout)
            .putAllAdditionalHeaders(tracingHeaders);
        
        if (header.getTargetServiceKey() != null) {
            if (!proxy.isSpi()) {
                throw new IllegalStateException(String.format("Trying to invoke %s (which is not an spi) with a key",
                    serviceName));
            }
            builder.setServiceKey(header.getTargetServiceKey());
        } else if (proxy.isSpi()) {
            throw new IllegalStateException(String.format("Trying to invoke %s (which is an spi) without a key",
                serviceName));
        }
        if (header.getTenantId() != null) {
            builder.setTenantId(header.getTenantId());
        }
        if (request != null) {
            builder.setPayload(request.toByteString());
        }
        
        LOG.debug("Sending request [{}:{}] for [{}]",
            builder.getCorrelationId(),
            builder.getCallRef(),
            proxy.serviceStubType);
        return invoke(builder.build()).map(re -> parse(re, methodInfo));
    }
    
    private <R extends MessageLite, E extends MessageLite> Object parse(final ResponseEnvelope responseEnvelope,
                                                                        final StubResourceMethodInfo<?, R, E> methodInfo) throws ClientConflictException {
        if (ResponseStatusCode.OK == responseEnvelope.getStatusCode()) {
            try {
                LOG.debug("Received response [{}:{}] for [{}]",
                    responseEnvelope.getCorrelationId(),
                    responseEnvelope.getCallRef(),
                    serviceName);
                if (methodInfo.responseType != null) {
                    return MessageHelper.parse(methodInfo.responseType,
                        responseEnvelope.getPayload(),
                        responseEnvelope.getJsonPayload());
                } else {
                    return Nil.getInstance();
                }
            } catch (final InvalidProtocolBufferException | RuntimeException e) {
                LOG.error("Received invalid response [{}:{}] for [{}]",
                    responseEnvelope.getCorrelationId(),
                    responseEnvelope.getCallRef(),
                    serviceName);
                throw new ServerErrorException(e);
            }
            
        } else if (ResponseStatusCode.CLIENT_CONFLICT == responseEnvelope.getStatusCode()) {
            try {
                LOG.debug("Received conflict [{}:{}] for [{}]",
                    responseEnvelope.getCorrelationId(),
                    responseEnvelope.getCallRef(),
                    serviceName);
                if (methodInfo.conflictType != null) {
                    // determine the correct ClientConflictException subclass to use
                    final E conflict = MessageHelper.parse(methodInfo.conflictType,
                        responseEnvelope.getPayload(),
                        responseEnvelope.getJsonPayload());
                    throw methodInfo.conflictConstructor.newInstance(conflict);
                } else {
                    throw new ServerErrorException("Got conflict but no conflict defined");
                }
            } catch (final ReflectiveOperationException | InvalidProtocolBufferException | RuntimeException e) {
                LOG.error("Received invalid conflict [{}:{}] for [{}]",
                    responseEnvelope.getCorrelationId(),
                    responseEnvelope.getCallRef(),
                    serviceName);
                throw new ServerErrorException(e);
            }
            
        } else if (ResponseStatusCode.SERVER_UNAVAILABLE == responseEnvelope.getStatusCode()) {
            LOG.error("Received server unavailable [{}:{}] for [{}]",
                responseEnvelope.getCorrelationId(),
                responseEnvelope.getCallRef(),
                serviceName);
            throw new ServerUnavailableException(responseEnvelope.getStatusMessage());
        } else {
            LOG.debug("Received fail [{}:{}] for [{}]",
                responseEnvelope.getStatusCode(),
                responseEnvelope.getCorrelationId(),
                serviceName);
            throw ServiceException.from(responseEnvelope.getStatusCode(),
                responseEnvelope.getStatusMessage(),
                responseEnvelope.getPayload(),
                responseEnvelope.getJsonPayload());
        }
    }
    
}
