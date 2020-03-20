package com.ixaris.commons.clustering.lib.service;

@FunctionalInterface
public interface ClusterLeadershipChangeListener {
    
    /**
     * @param leader true if this node became the leader, false otherwise
     */
    void onLeadershipChanged(boolean leader);
    
}
