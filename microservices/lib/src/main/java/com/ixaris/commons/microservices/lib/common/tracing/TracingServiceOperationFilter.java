package com.ixaris.commons.microservices.lib.common.tracing;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.rejected;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.microservices.lib.common.tracing.TracingUtil.ACTIVE_SPAN;
import static com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import static com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode.CLIENT_INVALID_REQUEST;
import static com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode.SERVER_ERROR;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.common.ServiceOperationFilter;
import com.ixaris.commons.microservices.lib.common.tracing.TracingServiceOperationReference.TracingServiceOperationReferenceBuilder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

/**
 * A {@link ServiceOperationFilter} that is used to build and activate a span for the current execution of a service operation.
 * Additionally, in order to be able to access this active span, it is stored in an {@link AsyncLocal}.
 *
 * @author <a href="mailto:jacob.falzon@ixaris.com">jacob.falzon</a>.
 */
public class TracingServiceOperationFilter implements ServiceOperationFilter {
    
    private static final String TRACING_TAG_STATUS_CODE = "STATUS_CODE";
    private static final String TRACING_BAGGAGE_KEY_CORRELATION_ID = "correlationId";
    private static final String TRACING_BAGGAGE_KEY_INTENT_ID = "intentId";
    
    @Override
    public Async<ResponseEnvelope> doFilter(final RequestEnvelope requestEnvelope, final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> next) {
        final Span span = initiateSpan(requestEnvelope);
        return ACTIVE_SPAN.exec(span, () -> {
            try {
                final ResponseEnvelope responseEnvelope = await(next.next(requestEnvelope));
                span.setTag(Tags.ERROR, responseEnvelope.getStatusCode().getNumber() >= CLIENT_INVALID_REQUEST.getNumber());
                span.setTag(TRACING_TAG_STATUS_CODE, responseEnvelope.getStatusCode().name());
                span.log(format("Status message: %s", responseEnvelope.getStatusMessage().isEmpty() ? "N/A" : responseEnvelope.getStatusMessage()));
                span.finish();
                return result(responseEnvelope);
            } catch (final Throwable e) {
                span.setTag(Tags.ERROR, true);
                span.setTag(TRACING_TAG_STATUS_CODE, SERVER_ERROR.name());
                span.log(format("Status message: %s", e.getMessage()));
                span.finish();
                return rejected(e);
            }
        });
    }
    
    private Span initiateSpan(final RequestEnvelope requestEnvelope) {
        final Tracer tracer = GlobalTracer.get();
        final Map<String, String> headersMap = new HashMap<>(requestEnvelope.getAdditionalHeadersMap());
        final SpanContext parentSpanContext = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapAdapter(headersMap));
        
        final TracingServiceOperationReference serviceReference = TracingServiceOperationReferenceBuilder.builder()
            .withServiceName(requestEnvelope.getServiceName())
            .withServiceKey(requestEnvelope.getServiceKey())
            .withMethod(requestEnvelope.getMethod())
            .build();
        
        final String spanOperationName = format("Microservice Call to %s", serviceReference.getServiceReference());
        final Span span = tracer.buildSpan(spanOperationName).asChildOf(parentSpanContext).start();
        
        span.setTag(Tags.PEER_SERVICE, serviceReference.getServiceReference());
        span.setTag(Tags.HTTP_URL, requestEnvelope.getPathList().toString());
        span.setTag(Tags.HTTP_METHOD, requestEnvelope.getMethod());
        span.setTag(Tags.SPAN_KIND, Tags.SPAN_KIND_SERVER);
        
        span.setBaggageItem(TRACING_BAGGAGE_KEY_CORRELATION_ID, String.valueOf(requestEnvelope.getCorrelationId()));
        span.setBaggageItem(TRACING_BAGGAGE_KEY_INTENT_ID, String.valueOf(requestEnvelope.getIntentId()));
        return span;
    }
}
