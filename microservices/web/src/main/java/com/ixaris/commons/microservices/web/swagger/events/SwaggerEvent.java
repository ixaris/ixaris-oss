package com.ixaris.commons.microservices.web.swagger.events;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;

/**
 * POJO to store details on the resolved request, parsed request and original request info for correlation/logging
 * purposes
 */
public final class SwaggerEvent {
    
    private final ServicePathHolder path;
    private final ServiceEventHeader<?> header;
    private final MessageLite event;
    
    public SwaggerEvent(final ServicePathHolder path, final ServiceEventHeader<?> header, final MessageLite event) {
        this.path = path;
        this.header = header;
        this.event = event;
    }
    
    public ServicePathHolder getPath() {
        return path;
    }
    
    public ServiceEventHeader<?> getHeader() {
        return header;
    }
    
    public MessageLite getEvent() {
        return event;
    }
    
}
