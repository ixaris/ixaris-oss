package com.ixaris.commons.microservices.scsl2openapi.lib;

import com.ixaris.commons.misc.lib.object.ToStringUtil;

/**
 * Descriptor on which methods are to be exposed in Swagger. This is the result of the pre-processing stage when interpreting SCSL methods.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class OpenAPIEventDescriptor {
    
    private final String description;
    private final String eventId;
    private final String httpPath;
    
    private final Class<?> eventType;
    
    OpenAPIEventDescriptor(final String description, final String eventId, final String httpPath, final Class<?> eventType) {
        
        this.description = description;
        this.eventId = eventId;
        this.httpPath = httpPath;
        this.eventType = eventType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public String getHttpPath() {
        return httpPath;
    }
    
    public Class<?> getEventType() {
        return eventType;
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("eventId", eventId).with("httpPath", httpPath).toString();
    }
    
}
