package com.ixaris.commons.microservices.lib.common.tracing;

import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilter;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.async.test.CompletionStageAssert;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;

/**
 * A unit test for {@link TracingServiceOperationFilter}.
 *
 * @author <a href="mailto:jacob.falzon@ixaris.com">jacob.falzon</a>.
 */
@RunWith(MockitoJUnitRunner.class)
public class TracingServiceOperationFilterTest {
    
    private final AsyncFilter<RequestEnvelope, ResponseEnvelope> filter = new TracingServiceOperationFilter();
    
    @Mock
    private AsyncFilterNext<RequestEnvelope, ResponseEnvelope> next;
    
    private final RequestEnvelope requestEnvelope = RequestEnvelope.newBuilder().build();
    private final ResponseEnvelope responseEnvelope = ResponseEnvelope.newBuilder().build();
    
    private final SpanContext parentSpan = mock(SpanContext.class);
    private final Span span = mock(Span.class);
    
    @Before
    public void setup() {
        when(next.next(requestEnvelope)).thenReturn(Async.result(responseEnvelope));
        
        mockParentSpan(MockTracer.getTracer());
        mockSpanCreation(MockTracer.getTracer());
    }
    
    @After
    public void teardown() {
        MockTracer.reset();
    }
    
    @Test
    public void doFilter_shouldExtractParentSpan() {
        join(filter.doFilter(requestEnvelope, next));
        
        verify(MockTracer.getTracer()).extract(eq(Format.Builtin.TEXT_MAP), any());
    }
    
    @Test
    public void doFilter_shouldCreateNewSpan() {
        join(filter.doFilter(requestEnvelope, next));
        
        verify(MockTracer.getTracer()).buildSpan(anyString());
    }
    
    @Test
    public void doFilter_operationCompletes_shouldFinishSpan() {
        join(filter.doFilter(requestEnvelope, next));
        
        verify(span).finish();
    }
    
    @Test
    public void doFilter_operationCompletes_shouldReturnResponseEnvelope() {
        final ResponseEnvelope response = join(filter.doFilter(requestEnvelope, next));
        
        assertThat(response).isEqualTo(responseEnvelope);
    }
    
    @Test
    public void doFilter_operationThrowsException_shouldFinishSpan() {
        CompletionStageAssert.assertThat(() -> filter.doFilter(requestEnvelope, request -> {
            throw new RuntimeException("error");
        })).await().isRejected();
        
        verify(span).finish();
    }
    
    @Test
    public void doFilter_operationThrowsException_shouldReThrowException() {
        assertThatThrownBy(() -> join(filter.doFilter(requestEnvelope, request -> {
            throw new RuntimeException("error");
        }))).isInstanceOf(RuntimeException.class);
    }
    
    private void mockParentSpan(final Tracer mockTracer) {
        when(mockTracer.extract(eq(Format.Builtin.TEXT_MAP), any(TextMapAdapter.class))).thenReturn(parentSpan);
    }
    
    private void mockSpanCreation(final Tracer mockTracer) {
        final Tracer.SpanBuilder spanBuilder = mock(Tracer.SpanBuilder.class);
        when(mockTracer.buildSpan(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.asChildOf(parentSpan)).thenReturn(spanBuilder);
        when(spanBuilder.start()).thenReturn(span);
    }
}
