package com.ixaris.commons.clustering.lib.service;

import java.util.Map;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;
import com.ixaris.commons.clustering.lib.common.ClusterShardResolver;
import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.misc.lib.function.CallableThrows;

/**
 * The service registry holds all the details on the available nodes, grouped by cluster name. Each node is responsible
 * of publishing the details of how other nodes can connect to it (using named attributes).
 * <p>
 * There are 2 main scenarios where nodes connect to each other:
 *
 * <ol>
 *   <li>Connecting with other nodes in the same cluster to route and broadcast messages within the cluster
 *   <li>Connecting with nodes in a cluster as a client
 * </ol>
 *
 * <p>
 * Implementations are responsible for leader tracking and shard allocation tracking. Implementations should also handle
 * unclean shutdown (crash)
 *
 * @author brian.vella
 * @author aldrin.seychell
 */
public interface ClusterRegistry extends ClusterShardResolver {
    
    AsyncLocal<Integer> SHARD = new AsyncLocal<>("clustering_shard");
    
    ClusterNodeInfo getNodeInfo();
    
    int getNodeId();
    
    boolean isLeader();
    
    ClusterTopology getTopology();
    
    /**
     * @param attributes the attributes to merge
     */
    void mergeAttributes(Map<String, String> attributes);
    
    /**
     * This is used to mark an in-flight operation on a shard.
     * <p>
     * Implementations should track these and notify that shard is stopped when all in-flight operations complete
     *
     * @throws ShardNotLocalException if the shard is not local
     */
    @SuppressWarnings("squid:S1160")
    <T, E extends Exception> Async<T> forShard(final int shard, final CallableThrows<Async<T>, E> callable) throws E, ShardNotLocalException;
    
    <REQ extends MessageLite, RES extends MessageLite> Async<RES> route(ClusterRouteHandler<REQ, RES> handler, String key, REQ request) throws ClusterRouteTimeoutException;
    
    <REQ extends MessageLite, RES extends MessageLite> Async<RES> route(ClusterRouteHandler<REQ, RES> handler, long key, REQ request) throws ClusterRouteTimeoutException;
    
    void register(ClusterRouteHandler<?, ?> handler);
    
    void deregister(ClusterRouteHandler<?, ?> handler);
    
    /**
     * broadcast to all other nodes in the cluster (except self). This is a best effort broadcast and due to the
     * volatility of network topologies, may not reach all the nodes.
     */
    <T extends MessageLite> Async<Boolean> broadcast(ClusterBroadcastHandler<T> handler, T message);
    
    void register(ClusterBroadcastHandler<?> handler);
    
    void deregister(ClusterBroadcastHandler<?> handler);
    
    /**
     * Add a listener to be notified of leadership changes. The listener is immediately notified of the current
     * leadership state, and for any subsequent change.
     *
     * @param listener the listener to set
     */
    void addLeadershipListener(ClusterLeadershipChangeListener listener);
    
    /**
     * Remove a listener. Given listener instance must match an added listener, otherwise request is ignored.
     *
     * @param listener the listener to unset
     */
    void removeLeadershipListener(ClusterLeadershipChangeListener listener);
    
    void addShardsListener(ClusterShardsChangeListener listener);
    
    void removeShardsListener(ClusterShardsChangeListener listener);
    
}
