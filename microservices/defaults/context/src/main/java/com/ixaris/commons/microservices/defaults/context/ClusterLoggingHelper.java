package com.ixaris.commons.microservices.defaults.context;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterRequestEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterResponseEnvelope;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.lib.common.ServiceHeader;

public final class ClusterLoggingHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(ClusterLoggingHelper.class);
    
    public static Async<ClusterResponseEnvelope> logRoute(final ClusterRequestEnvelope in,
                                                          final AsyncFilterNext<ClusterRequestEnvelope, ClusterResponseEnvelope> next,
                                                          final ServiceHeader<Context> header,
                                                          final String desc) {
        final String details = String.format(
            "%s/%s [%s], %s %d:%d %d",
            header.getServiceName(),
            header.getServiceKey(),
            in.getType(),
            header.getTenantId(),
            header.getCorrelationId(),
            header.getCallRef(),
            header.getIntentId());
        
        LOG.info("{} Route: {}", desc, details);
        
        final ClusterResponseEnvelope out = await(next.next(in));
        
        if (!out.getTimeout()) {
            if (out.getExceptionClass().isEmpty()) {
                LOG.info("{} RouteAck: {}, Status: OK", desc, details);
            } else {
                LOG.error(
                    "{} RouteAck: {}, Status: FAILED:{}:{}",
                    desc,
                    details,
                    out.getExceptionClass(),
                    out.getExceptionMessage());
            }
        } else {
            LOG.error("{} : {}, Status: TIMEOUT", desc, details);
        }
        
        return result(out);
    }
    
    public static Async<Boolean> logBroadcast(
                                              final ClusterBroadcastEnvelope in,
                                              final AsyncFilterNext<ClusterBroadcastEnvelope, Boolean> next,
                                              final ServiceHeader<Context> header,
                                              final String desc) {
        final String details = String.format(
            "%s/%s [%s], %s %d:%d %d",
            header.getServiceName(),
            header.getServiceKey(),
            in.getType(),
            header.getTenantId(),
            header.getCorrelationId(),
            header.getCallRef(),
            header.getIntentId());
        
        LOG.info("{} Broadcast: {}", desc, details);
        
        final Boolean out = await(next.next(in));
        
        if ((out != null) && out) {
            LOG.info("{} BroadcastAck: {}, Status: OK", desc, details);
        } else {
            LOG.error("{} BroadcastAck: {}, Status: FAILED", desc, details);
        }
        
        return result(out);
    }
    
    private ClusterLoggingHelper() {}
    
}
