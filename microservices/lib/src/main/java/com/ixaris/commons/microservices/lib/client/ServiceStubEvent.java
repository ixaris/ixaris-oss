package com.ixaris.commons.microservices.lib.client;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.client.ServiceEvent.BackpressureException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;

public interface ServiceStubEvent {
    
    static EventAckEnvelope wrapError(final EventEnvelope eventEnvelope, final ServiceException e) {
        final EventAckEnvelope.Builder builder = EventAckEnvelope.newBuilder()
            .setCorrelationId(eventEnvelope.getCorrelationId())
            .setCallRef(eventEnvelope.getCallRef())
            .setStatusCode(e.getStatusCode());
        
        final String statusMessage = e.getMessage();
        if (statusMessage != null) {
            builder.setStatusMessage(statusMessage);
        }
        return builder.build();
    }
    
    Async<EventAckEnvelope> invokeOnListener();
    
    ServiceEvent<?, ?> getResourceEventObject() throws BackpressureException;
    
    EventAckEnvelope wrapError(final ServiceException e);
    
    Class<?> getResourceType();
    
    EventEnvelope getEventEnvelope();
    
    MessageLite getEvent();
    
}
