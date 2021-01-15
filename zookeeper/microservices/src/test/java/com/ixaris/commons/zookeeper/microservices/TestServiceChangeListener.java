package com.ixaris.commons.zookeeper.microservices;

import java.util.concurrent.atomic.AtomicReference;

import com.ixaris.commons.microservices.lib.client.discovery.ServiceEndpoint;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceEndpointChangeListener;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class TestServiceChangeListener implements ServiceEndpointChangeListener {
    
    private final AtomicReference<ServiceEndpoint> topologyChanges = new AtomicReference<>();
    
    @Override
    public void onEndpointChanged(final ServiceEndpoint endpoint) {
        topologyChanges.lazySet(endpoint);
    }
    
    ServiceEndpoint getLatestTopology() {
        return topologyChanges.get();
    }
}
