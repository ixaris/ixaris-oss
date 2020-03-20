package com.ixaris.commons.microservices.lib.common.tracing;

import static org.mockito.Mockito.mock;

import org.mockito.Mockito;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

/**
 * Stores a mocked {@link Tracer} instance that is registered and accessible through {@link GlobalTracer#get}.
 *
 * @author <a href="mailto:jacob.falzon@ixaris.com">jacob.falzon</a>.
 */
public final class MockTracer {
    
    private static final Tracer TRACER = initMockTracer();
    
    private MockTracer() {}
    
    public static Tracer getTracer() {
        return TRACER;
    }
    
    public static void reset() {
        Mockito.clearInvocations(TRACER);
    }
    
    private static Tracer initMockTracer() {
        final Tracer mockTracer = mock(Tracer.class);
        GlobalTracer.registerIfAbsent(mockTracer);
        return mockTracer;
    }
}
