package com.ixaris.commons.jooq.microservices.support;

import java.util.Map;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;
import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.service.ClusterBroadcastHandler;
import com.ixaris.commons.clustering.lib.service.ClusterLeadershipChangeListener;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ClusterRouteHandler;
import com.ixaris.commons.clustering.lib.service.ClusterShardsChangeListener;
import com.ixaris.commons.collections.lib.BitSet;
import com.ixaris.commons.misc.lib.function.CallableThrows;

public class TestClusterRegistry implements ClusterRegistry {
    
    private final ClusterTopology topology = new ClusterTopology(this);
    private final ClusterNodeInfo nodeInfo = new ClusterNodeInfo(System.currentTimeMillis(), BitSet.of(1, 0), BitSet.of(1), null);
    
    @Override
    public int getNodeId() {
        return 0;
    }
    
    @Override
    public ClusterNodeInfo getNodeInfo() {
        return nodeInfo;
    }
    
    @Override
    public boolean isLeader() {
        return true;
    }
    
    @Override
    public ClusterTopology getTopology() {
        return topology;
    }
    
    @Override
    public void mergeAttributes(final Map<String, String> attributes) {}
    
    @Override
    public <T, E extends Exception> Async<T> forShard(final int shard, final CallableThrows<Async<T>, E> callable) throws E {
        return SHARD.exec(shard, callable);
    }
    
    @Override
    public <REQ extends MessageLite, RES extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                               final String key,
                                                                               final REQ request) {
        return route(handler, 0L, key, request);
    }
    
    @Override
    public <REQ extends MessageLite, RES extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                               final long id,
                                                                               final REQ request) {
        return route(handler, id, "", request);
    }
    
    private <REQ extends MessageLite, RES extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                                final long id,
                                                                                final String key,
                                                                                final REQ request) {
        return forShard(0, () -> handler.handle(id, key, request));
    }
    
    @Override
    public void register(final ClusterRouteHandler<?, ?> handler) {}
    
    @Override
    public void deregister(final ClusterRouteHandler<?, ?> handler) {}
    
    @Override
    public <T extends MessageLite> Async<Boolean> broadcast(final ClusterBroadcastHandler<T> handler, final T message) {
        return handler.handle(message);
    }
    
    @Override
    public void register(final ClusterBroadcastHandler<?> handler) {}
    
    @Override
    public void deregister(final ClusterBroadcastHandler<?> handler) {}
    
    @Override
    public void addLeadershipListener(final ClusterLeadershipChangeListener listener) {
        listener.onLeadershipChanged(true);
    }
    
    @Override
    public void removeLeadershipListener(final ClusterLeadershipChangeListener listener) {}
    
    @Override
    public void addShardsListener(final ClusterShardsChangeListener listener) {
        listener.onShardsChanged(BitSet.empty(), nodeInfo.getShards());
    }
    
    @Override
    public void removeShardsListener(final ClusterShardsChangeListener listener) {}
    
    @Override
    public int getMaxShards() {
        return 1;
    }
    
    @Override
    public int getShard(final long id) {
        return 0;
    }
    
}
