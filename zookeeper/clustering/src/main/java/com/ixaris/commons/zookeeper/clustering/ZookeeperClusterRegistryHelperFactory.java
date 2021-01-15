package com.ixaris.commons.zookeeper.clustering;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterRequestEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterResponseEnvelope;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterRegistryHelper.ShardStoppedHandler;

public interface ZookeeperClusterRegistryHelperFactory {
    
    ZookeeperClusterRegistryHelper create(ClusterRegistry clusterRegistry,
                                          int nodeId,
                                          ShardStoppedHandler shardStoppedHandler,
                                          AsyncFilterNext<ClusterRequestEnvelope, ClusterResponseEnvelope> handleRouteChain,
                                          AsyncFilterNext<ClusterBroadcastEnvelope, Boolean> handleBroadcastChain);
    
}
