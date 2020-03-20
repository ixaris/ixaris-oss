package com.ixaris.commons.microservices.lib.service.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.common.EventAck;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.lib.common.exception.ServerUnavailableException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.protobuf.lib.MessageValidator;

import valid.Valid.MessageValidation;

public final class ServiceSkeletonEventPublisher<T extends ServiceSkeleton> {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceSkeletonEventPublisher.class);
    
    private final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain;
    private final ServiceSkeletonProxy<T> proxy;
    private final String serviceKey;
    private final String serviceName;
    
    ServiceSkeletonEventPublisher(final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain,
                                  final ServiceSkeletonProxy<T> proxy,
                                  final String serviceKey) {
        if (filterChain == null) {
            throw new IllegalArgumentException("filterChain is null");
        }
        if (proxy == null) {
            throw new IllegalArgumentException("proxy is null");
        }
        
        this.filterChain = filterChain;
        this.proxy = proxy;
        this.serviceKey = serviceKey;
        serviceName = ServiceSkeleton.extractServiceName(proxy.serviceSkeletonType);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + proxy.serviceSkeletonType;
    }
    
    Async<EventAck> publish(final ServiceEventHeader<?> header, final MessageLite event, final ServicePathHolder path) {
        return publishEnvelope(buildEnvelope(header, event, path));
    }
    
    public Async<EventAck> publishEnvelope(final EventEnvelope eventEnvelope) {
        return filterChain.next(eventEnvelope).map(this::parse);
    }
    
    public EventEnvelope buildEnvelope(final ServiceEventHeader<?> header,
                                       final MessageLite event,
                                       final ServicePathHolder path) throws ClientInvalidRequestException {
        final MessageValidation validation = MessageValidator.validate(event);
        
        if (validation.getInvalid()) {
            throw new ClientInvalidRequestException(validation);
        }
        
        // transform header and request in envelope
        final EventEnvelope.Builder builder = EventEnvelope.newBuilder()
            .setCorrelationId(header.getCorrelationId())
            .setCallRef(UniqueIdGenerator.generate())
            .setParentRef(header.getCallRef())
            .setServiceName(serviceName)
            .setServiceKey(serviceKey) // ignore the service key in the header and assign own key
            .addAllPath(path)
            .setPartitionId(header.getPartitionId())
            .setIntentId(header.getIntentId())
            .setContext(header.getContext().toByteString())
            .setPayload(event.toByteString());
        
        if (header.getTenantId() != null) {
            builder.setTenantId(header.getTenantId());
        }
        
        return builder.build();
    }
    
    private EventAck parse(final EventAckEnvelope eventAckEnvelope) {
        if (ResponseStatusCode.OK == eventAckEnvelope.getStatusCode()) {
            LOG.debug("Received eventAck [{}:{}] for [{}]",
                eventAckEnvelope.getCorrelationId(),
                eventAckEnvelope.getCallRef(),
                serviceName);
            return EventAck.getInstance();
            
        } else if (ResponseStatusCode.SERVER_UNAVAILABLE == eventAckEnvelope.getStatusCode()) {
            LOG.error("Received server unavailable [{}:{}] for [{}]",
                eventAckEnvelope.getCorrelationId(),
                eventAckEnvelope.getCallRef(),
                serviceName);
            throw new ServerUnavailableException(eventAckEnvelope.getStatusMessage());
        } else {
            LOG.debug("Received fail [{}:{}] for [{}]",
                eventAckEnvelope.getStatusCode(),
                eventAckEnvelope.getCorrelationId(),
                serviceName);
            throw ServiceException.from(eventAckEnvelope.getStatusCode(),
                eventAckEnvelope.getStatusMessage(),
                null,
                false);
        }
    }
    
}
