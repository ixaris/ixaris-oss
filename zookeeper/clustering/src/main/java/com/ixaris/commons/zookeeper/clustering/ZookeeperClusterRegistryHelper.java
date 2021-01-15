package com.ixaris.commons.zookeeper.clustering;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterRequestEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterResponseEnvelope;
import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.service.ClusterRouteHandler;
import com.ixaris.commons.clustering.lib.service.ClusterRouteTimeoutException;
import com.ixaris.commons.clustering.lib.service.ShardNotLocalException;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;

public abstract class ZookeeperClusterRegistryHelper {
    
    public interface ShardStoppedHandler {
        
        Async<Void> handleShardStopped(int nodeId, int shard);
        
    }
    
    @FunctionalInterface
    public interface DefaultRoute {
        
        <REQ extends MessageLite, RES extends MessageLite> Async<RES> defaultRoute(final ClusterRouteHandler<REQ, RES> handler,
                                                                                   final long id,
                                                                                   final String key,
                                                                                   final REQ request,
                                                                                   final FunctionThrows<ClusterRequestEnvelope, Async<ClusterResponseEnvelope>, ?> route) throws ClusterRouteTimeoutException;
        
    }
    
    protected final int nodeId;
    
    protected ZookeeperClusterRegistryHelper(final int nodeId) {
        this.nodeId = nodeId;
    }
    
    public abstract void updateTopology(ClusterTopology clusterTopology);
    
    @SuppressWarnings("squid:S1160")
    public abstract <T, E extends Exception> Async<T> forShard(final int shard, final CallableThrows<Async<T>, E> callable) throws E, ShardNotLocalException;
    
    public abstract <RES extends MessageLite, REQ extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                                        final long id,
                                                                                        final String key,
                                                                                        final REQ request,
                                                                                        final DefaultRoute defaultRoute) throws ClusterRouteTimeoutException;
    
    /**
     * broadcast to all other nodes in the cluster (except self). This is a best effort broadcast and due to the
     * volatility of network topologies, may not reach all the nodes.
     */
    public abstract Async<Boolean> broadcast(ClusterBroadcastEnvelope message);
    
    public abstract void shutdown();
    
}
