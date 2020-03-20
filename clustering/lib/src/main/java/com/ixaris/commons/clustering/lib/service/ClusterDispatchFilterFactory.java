package com.ixaris.commons.clustering.lib.service;

import com.ixaris.commons.misc.lib.object.Ordered;

public interface ClusterDispatchFilterFactory extends Ordered {
    
    default ClusterRouteFilter createRouteFilter() {
        return null;
    }
    
    default ClusterBroadcastFilter createBroadcastFilter() {
        return null;
    }
    
}
