package com.ixaris.commons.microservices.lib.common.tracing;

import static java.lang.String.format;

import java.util.Objects;

import io.opentracing.Span;

/**
 * A convenience class that is used to assemble the service reference when sending/receiving microservice operation requests.
 * This reference is used to attach to new or existing {@link Span}s.
 *
 * @author <a href="mailto:jacob.falzon@ixaris.com">jacob.falzon</a>.
 */
public final class TracingServiceOperationReference {
    
    private final String serviceReference;
    
    private TracingServiceOperationReference(final String serviceName, final String serviceKey, final String method) {
        serviceReference = format("%s%s/%s", serviceName, Objects.isNull(serviceKey) || serviceKey.isEmpty() ? "" : ('/' + serviceKey), method);
    }
    
    public String getServiceReference() {
        return serviceReference;
    }
    
    public static final class TracingServiceOperationReferenceBuilder {
        
        private String serviceName;
        private String serviceKey; // applies only for SPIs
        private String method;
        
        private TracingServiceOperationReferenceBuilder() {}
        
        public static TracingServiceOperationReferenceBuilder builder() {
            return new TracingServiceOperationReferenceBuilder();
        }
        
        public TracingServiceOperationReferenceBuilder withServiceName(final String serviceName) {
            this.serviceName = serviceName;
            return this;
        }
        
        public TracingServiceOperationReferenceBuilder withServiceKey(final String serviceKey) {
            this.serviceKey = serviceKey;
            return this;
        }
        
        public TracingServiceOperationReferenceBuilder withMethod(final String method) {
            this.method = method;
            return this;
        }
        
        public TracingServiceOperationReference build() {
            return new TracingServiceOperationReference(serviceName, serviceKey, method);
        }
    }
}
