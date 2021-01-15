package com.ixaris.commons.zookeeper.clustering;

import static com.ixaris.commons.clustering.lib.service.ClusterRegistry.SHARD;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterRequestEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterResponseEnvelope;
import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.service.ClusterRouteHandler;
import com.ixaris.commons.misc.lib.function.CallableThrows;

public class LocalClusterRegistryHelper extends ZookeeperClusterRegistryHelper {
    
    private final AsyncFilterNext<ClusterRequestEnvelope, ClusterResponseEnvelope> handleRouteChain;
    private final AsyncFilterNext<ClusterBroadcastEnvelope, Boolean> handleBroadcastChain;
    
    public LocalClusterRegistryHelper(final int nodeId,
                                      final AsyncFilterNext<ClusterRequestEnvelope, ClusterResponseEnvelope> handleRouteChain,
                                      final AsyncFilterNext<ClusterBroadcastEnvelope, Boolean> handleBroadcastChain) {
        super(nodeId);
        this.handleRouteChain = handleRouteChain;
        this.handleBroadcastChain = handleBroadcastChain;
    }
    
    @Override
    public void updateTopology(final ClusterTopology clusterTopology) {
        // no logic for local
    }
    
    @Override
    public <T, E extends Exception> Async<T> forShard(final int shard, final CallableThrows<Async<T>, E> callable) throws E {
        return SHARD.exec(shard, callable);
    }
    
    @Override
    public <RES extends MessageLite, REQ extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                               final long id,
                                                                               final String key,
                                                                               final REQ request,
                                                                               final DefaultRoute defaultRoute) {
        return forShard(0, () -> handler.handle(id, key, request));
    }
    
    @Override
    public Async<Boolean> broadcast(final ClusterBroadcastEnvelope message) {
        return handleBroadcastChain.next(message);
    }
    
    @Override
    public void shutdown() {
        // no logic for local
    }
    
}
