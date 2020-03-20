package com.ixaris.commons.microservices.lib.service;

import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.ServiceOperation.BackpressureException;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;

public interface ServiceSkeletonOperation {
    
    static ResponseEnvelope wrapError(final RequestEnvelope requestEnvelope, final ServiceException e) {
        final ResponseEnvelope.Builder builder = ResponseEnvelope.newBuilder()
            .setCorrelationId(requestEnvelope.getCorrelationId())
            .setCallRef(requestEnvelope.getCallRef())
            .setStatusCode(e.getStatusCode());
        
        final String statusMessage = e.getMessage();
        if (statusMessage != null) {
            builder.setStatusMessage(statusMessage);
        }
        final ByteString payload = e.getPayload(requestEnvelope.getJsonPayload());
        if (payload != null) {
            builder.setPayload(payload);
        }
        if (requestEnvelope.getJsonPayload()) {
            builder.setJsonPayload(true);
        }
        return builder.build();
    }
    
    Async<ResponseEnvelope> invokeOnResourceProxy();
    
    ServiceOperation<?, ?, ?, ?> getResourceOperationObject() throws BackpressureException;
    
    ResponseEnvelope wrapError(final ServiceException e);
    
    Class<?> getResourceType();
    
    List<Object> getParams();
    
    Long getShardKey();
    
    RequestEnvelope getRequestEnvelope();
    
    MessageLite getRequest();
    
    @Deprecated
    ServiceSkeletonProxy<?> getProxy();
    
}
