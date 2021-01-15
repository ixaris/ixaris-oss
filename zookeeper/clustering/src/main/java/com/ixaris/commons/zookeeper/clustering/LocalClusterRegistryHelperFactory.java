package com.ixaris.commons.zookeeper.clustering;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterRequestEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterResponseEnvelope;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterRegistryHelper.ShardStoppedHandler;

public final class LocalClusterRegistryHelperFactory implements ZookeeperClusterRegistryHelperFactory {
    
    @Override
    public ZookeeperClusterRegistryHelper create(final ClusterRegistry clusterRegistry,
                                                 final int nodeId,
                                                 final ShardStoppedHandler shardStoppedHandler,
                                                 final AsyncFilterNext<ClusterRequestEnvelope, ClusterResponseEnvelope> handleRouteChain,
                                                 final AsyncFilterNext<ClusterBroadcastEnvelope, Boolean> handleBroadcastChain) {
        return new LocalClusterRegistryHelper(nodeId, handleRouteChain, handleBroadcastChain);
    }
    
}
