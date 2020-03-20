package com.ixaris.commons.microservices.lib.common.tracing;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

import java.util.HashMap;
import java.util.Map;

import com.ixaris.commons.async.lib.AsyncLocal;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.util.GlobalTracer;

/**
 * A utility class that is responsible for maintaining a reference to the current active span as well as
 * building the associated headers that are to be used to transmit the {@link SpanContext} over an RPC.
 *
 * @author <a href="mailto:jacob.falzon@ixaris.com">jacob.falzon</a>.
 */
public final class TracingUtil {
    
    /**
     * The reference of the current active span wrapped in an {@link AsyncLocal}.
     */
    public static final AsyncLocal<Span> ACTIVE_SPAN = new AsyncLocal<>("ACTIVE_SPAN");
    
    private TracingUtil() {}
    
    public static Map<String, String> getTracingHeaders(final TracingServiceOperationReference serviceReference) {
        final Map<String, String> headers = new HashMap<>();
        final Span activeSpan = ACTIVE_SPAN.get();
        
        if (nonNull(activeSpan)) {
            GlobalTracer.get().inject(activeSpan.context(), Format.Builtin.TEXT_MAP, new TextMapAdapter(headers));
            activeSpan.log(format("Starting Microservice Call to %s", serviceReference.getServiceReference()));
        }
        
        return headers;
    }
}
