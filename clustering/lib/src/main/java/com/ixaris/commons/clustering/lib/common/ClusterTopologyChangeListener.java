package com.ixaris.commons.clustering.lib.common;

/**
 * Cluster topology change listeners that gets notified every time cluster topology (set of nodes available) changes.
 *
 * @author aldrin.seychell
 */
@FunctionalInterface
public interface ClusterTopologyChangeListener {
    
    /**
     * The listening party is expected to track the currently connected state and update internal state accordingly by
     * comparing with the current cluster topology (obtained through this listener). It is recommended that topology
     * changes are treated in a thread safe manner (e.g. queueing). This method SHOULD NOT BLOCK!
     *
     * <p>If the code to process the updated topology is not asynchronous, the recommended implementation stores last
     * known topology update in an AtomicReference and asynchronously process by invoking getAndSet(null) and comparing
     * to the current state.
     *
     * @param clusterTopology the new topology, never null (will be empty if service is no longer available)
     */
    void onTopologyChanged(ClusterTopology clusterTopology);
    
}
