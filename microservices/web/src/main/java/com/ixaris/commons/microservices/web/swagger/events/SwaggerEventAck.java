package com.ixaris.commons.microservices.web.swagger.events;

/**
 * POJO to store response, including parsed response. This can be mutated between different filters e.g. for
 * tokenisation
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class SwaggerEventAck {
    
    public static final class Status {
        
        private final boolean found;
        private final boolean applicable;
        
        public Status(final boolean found, final boolean applicable) {
            this.found = found;
            this.applicable = applicable;
        }
        
    }
    
    private final SwaggerEvent swaggerEvent;
    private final Status status;
    private final Throwable throwable;
    
    public SwaggerEventAck(final SwaggerEvent swaggerEvent, final Status status, final Throwable throwable) {
        this.swaggerEvent = swaggerEvent;
        this.status = status;
        this.throwable = throwable;
    }
    
    public SwaggerEvent getSwaggerEvent() {
        return swaggerEvent;
    }
    
    public boolean isFound() {
        return status.found;
    }
    
    public boolean isApplicable() {
        return status.applicable;
    }
    
    public Throwable getThrowable() {
        return throwable;
    }
    
}
