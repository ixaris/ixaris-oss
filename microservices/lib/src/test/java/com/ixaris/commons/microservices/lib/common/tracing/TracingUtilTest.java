package com.ixaris.commons.microservices.lib.common.tracing;

import static com.ixaris.commons.microservices.lib.common.tracing.TracingUtil.ACTIVE_SPAN;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.ixaris.commons.microservices.lib.common.tracing.TracingServiceOperationReference.TracingServiceOperationReferenceBuilder;

import io.opentracing.Span;
import io.opentracing.propagation.Format;

/**
 * A unit test for {@link TracingUtil}.
 *
 * @author <a href="mailto:jacob.falzon@ixaris.com">jacob.falzon</a>.
 */
@RunWith(MockitoJUnitRunner.class)
public class TracingUtilTest {
    
    private static final String SERVICE_NAME = "SERVICE_NAME";
    private static final String SERVICE_KEY = "SERVICE_KEY";
    private static final String METHOD = "METHOD";
    
    private final TracingServiceOperationReference serviceOperationReference = TracingServiceOperationReferenceBuilder.builder()
        .withServiceName(SERVICE_NAME)
        .withServiceKey(SERVICE_KEY)
        .withMethod(METHOD)
        .build();
    
    private final Span span = mock(Span.class);
    
    @After
    public void teardown() {
        MockTracer.reset();
    }
    
    @Test
    public void getTracingHeaders_activeSpanNotSet_shouldReturnEmptyHeadersMap() {
        assertThat(TracingUtil.getTracingHeaders(serviceOperationReference)).isEmpty();
    }
    
    @Test
    public void getTracingHeaders_activeSpanSet_shouldInjectActiveSpanContextInHeaders() {
        ACTIVE_SPAN.exec(span, () -> TracingUtil.getTracingHeaders(serviceOperationReference));
        
        verify(MockTracer.getTracer()).inject(any(), eq(Format.Builtin.TEXT_MAP), any());
    }
    
    @Test
    public void getTracingHeaders_activeSpanSet_microserviceCallWithServiceKey_shouldAddLogToActiveSpan() {
        ACTIVE_SPAN.exec(span, () -> TracingUtil.getTracingHeaders(serviceOperationReference));
        
        verify(span).log(format("Starting Microservice Call to %s/%s/%s", SERVICE_NAME, SERVICE_KEY, METHOD));
    }
    
    @Test
    public void getTracingHeaders_activeSpanSet_microserviceCallWithoutServiceKey_shouldAddLogToActiveSpan() {
        ACTIVE_SPAN.exec(span, () -> TracingUtil.getTracingHeaders(TracingServiceOperationReferenceBuilder.builder()
            .withServiceName(SERVICE_NAME)
            .withMethod(METHOD)
            .build()));
        
        verify(span).log(format("Starting Microservice Call to %s/%s", SERVICE_NAME, METHOD));
    }
}
