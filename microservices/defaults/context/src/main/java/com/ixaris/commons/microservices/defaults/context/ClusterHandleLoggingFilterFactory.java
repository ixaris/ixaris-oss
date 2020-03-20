package com.ixaris.commons.microservices.defaults.context;

import static com.ixaris.commons.clustering.lib.service.ClusterRegistry.SHARD;
import static com.ixaris.commons.microservices.defaults.context.AsyncLocals.HEADER;

import org.springframework.stereotype.Component;

import com.ixaris.commons.clustering.lib.service.ClusterBroadcastFilter;
import com.ixaris.commons.clustering.lib.service.ClusterHandleFilterFactory;
import com.ixaris.commons.clustering.lib.service.ClusterRouteFilter;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.lib.common.ServiceHeader;

@Component
public final class ClusterHandleLoggingFilterFactory implements ClusterHandleFilterFactory {
    
    private final ClusterRouteFilter routeFilter;
    private final ClusterBroadcastFilter broadcastFilter;
    
    public ClusterHandleLoggingFilterFactory() {
        routeFilter = (in, next) -> {
            if (SHARD.get() == null) {
                final ServiceHeader<Context> header = HEADER.get();
                if (header != null) {
                    return ClusterLoggingHelper.logRoute(in, next, header, "Handle");
                }
            }
            return next.next(in);
        };
        broadcastFilter = (in, next) -> {
            if (SHARD.get() == null) {
                final ServiceHeader<Context> header = HEADER.get();
                if (header != null) {
                    return ClusterLoggingHelper.logBroadcast(in, next, header, "Handle");
                }
            }
            return next.next(in);
        };
    }
    
    @Override
    public ClusterRouteFilter createRouteFilter() {
        return routeFilter;
    }
    
    @Override
    public ClusterBroadcastFilter createBroadcastFilter() {
        return broadcastFilter;
    }
    
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
    
}
