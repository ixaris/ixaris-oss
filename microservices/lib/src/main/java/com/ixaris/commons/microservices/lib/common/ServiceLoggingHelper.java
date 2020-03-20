package com.ixaris.commons.microservices.lib.common;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;

public final class ServiceLoggingHelper {
    
    public static final String KEY_SERVICE_NAME = "SERVICE_NAME";
    public static final String KEY_SERVICE_KEY = "SERVICE_KEY";
    public static final String KEY_PATH = "PATH";
    public static final String KEY_PATH_PARAMS = "PATH_PARAMS";
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceLoggingHelper.class);
    
    public static Async<ResponseEnvelope> logOperation(final RequestEnvelope in,
                                                       final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> next,
                                                       final String desc,
                                                       final Supplier<String> channelSupplier) {
        final String details = String.format("%s/%s [%s] %s, %s %d:%d %d %s",
            in.getServiceName(),
            in.getServiceKey(),
            ServicePathHolder.of(in.getPathList(), in.getParamsList()),
            in.getMethod(),
            in.getTenantId(),
            in.getCorrelationId(),
            in.getCallRef(),
            in.getIntentId(),
            channelSupplier != null ? channelSupplier.get() : "");
        final long start = System.nanoTime();
        
        LOG.info("{} Request: {}", desc, details);
        
        final ResponseEnvelope out = await(next.next(in));
        
        final long duration = (System.nanoTime() - start) / 1000000L;
        switch (ServiceConstants.resolveStatusClass(out.getStatusCode())) {
            case OK:
                LOG.info("{} Response: {}, Duration: {}ms, Status: OK", desc, details, duration);
                break;
            case CLIENT_ERROR:
                LOG.warn("{} Response: {}, Duration: {}ms, Status: {}:{}",
                    desc,
                    details,
                    duration,
                    out.getStatusCode(),
                    out.getStatusMessage());
                break;
            case SERVER_ERROR:
            default:
                LOG.error("{} Response: {}, Duration: {}ms, Status: {}:{}",
                    desc,
                    details,
                    duration,
                    out.getStatusCode(),
                    out.getStatusMessage());
                break;
        }
        
        return result(out);
    }
    
    public static Async<EventAckEnvelope> logEvent(final EventEnvelope in,
                                                   final AsyncFilterNext<EventEnvelope, EventAckEnvelope> next,
                                                   final String desc,
                                                   final Supplier<String> channelSupplier) {
        final String details = String.format("%s/%s [%s], %s %d:%d %d %s",
            in.getServiceName(),
            in.getServiceKey(),
            String.join("/", in.getPathList()),
            in.getTenantId(),
            in.getCorrelationId(),
            in.getCallRef(),
            in.getIntentId(),
            channelSupplier != null ? channelSupplier.get() : "");
        final long start = System.nanoTime();
        
        LOG.info("{} Event: {}", desc, details);
        
        final EventAckEnvelope out = await(next.next(in));
        
        final long duration = (System.nanoTime() - start) / 1000000L;
        switch (ServiceConstants.resolveStatusClass(out.getStatusCode())) {
            case OK:
                LOG.info("{} EventAck: {}, Duration: {}ms, Status: OK", desc, details, duration);
                break;
            case CLIENT_ERROR:
                LOG.warn("{} EventAck: {}, Duration: {}ms, Status: {}:{}",
                    desc,
                    details,
                    duration,
                    out.getStatusCode(),
                    out.getStatusMessage());
                break;
            case SERVER_ERROR:
            default:
                LOG.error("{} EventAck: {}, Duration: {}ms, Status: {}:{}",
                    desc,
                    details,
                    duration,
                    out.getStatusCode(),
                    out.getStatusMessage());
                break;
        }
        
        return result(out);
    }
    
    private ServiceLoggingHelper() {}
    
}
