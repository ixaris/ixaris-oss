package com.ixaris.commons.zookeeper.clustering;

import java.util.concurrent.atomic.AtomicReference;

import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.common.ClusterTopologyChangeListener;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class TestTopologyChangeListener implements ClusterTopologyChangeListener {
    
    private final AtomicReference<ClusterTopology> topologyChanges = new AtomicReference<>();
    
    @Override
    public void onTopologyChanged(final ClusterTopology topology) {
        topologyChanges.set(topology);
    }
    
    ClusterTopology getLatestTopology() {
        return topologyChanges.get();
    }
    
    ClusterTopology getAndClearLatestTopology() {
        return topologyChanges.getAndSet(null);
    }
    
}
