package com.ixaris.commons.zookeeper.microservices;

import java.util.concurrent.atomic.AtomicReference;

import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.common.ClusterTopologyChangeListener;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class TestNodeTopologyChangeListener implements ClusterTopologyChangeListener {
    
    private final AtomicReference<ClusterTopology> latestTopology = new AtomicReference<>();
    
    @Override
    public void onTopologyChanged(final ClusterTopology clusterTopology) {
        latestTopology.set(clusterTopology);
    }
    
    ClusterTopology getLatestTopology() {
        return latestTopology.get();
    }
    
    ClusterTopology getAndClearLatestTopology() {
        return latestTopology.getAndSet(null);
    }
    
}
