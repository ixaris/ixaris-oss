package com.ixaris.commons.netty.clustering;

import java.util.concurrent.ScheduledExecutorService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterRequestEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterResponseEnvelope;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterRegistryHelper.ShardStoppedHandler;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterRegistryHelperFactory;

public final class NettyClusterShardingFactory implements ZookeeperClusterRegistryHelperFactory {
    
    private final NettyBean nettyBean;
    private final ScheduledExecutorService executor;
    
    public NettyClusterShardingFactory(final NettyBean nettyBean, final ScheduledExecutorService executor) {
        this.nettyBean = nettyBean;
        this.executor = executor;
    }
    
    @Override
    public NettyClusterSharding create(final ClusterRegistry clusterRegistry,
                                       final int nodeId,
                                       final ShardStoppedHandler shardStoppedHandler,
                                       final AsyncFilterNext<ClusterRequestEnvelope, ClusterResponseEnvelope> handleRouteChain,
                                       final AsyncFilterNext<ClusterBroadcastEnvelope, Boolean> handleBroadcastChain) {
        return new NettyClusterSharding(nettyBean, executor, clusterRegistry, nodeId, shardStoppedHandler, handleRouteChain, handleBroadcastChain);
    }
}
