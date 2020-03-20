package com.ixaris.commons.clustering.lib.common;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

/**
 * An immutable representation of an endpoint topology
 */
public final class ClusterTopology {
    
    public static final int UNKNOWN_NODE_ID = -1;
    
    private final SortedMap<Integer, ClusterNodeInfo> nodes;
    private final int leaderNodeId;
    private final ClusterShardResolver shardResolver;
    
    public ClusterTopology(final ClusterShardResolver shardResolver) {
        this(null, UNKNOWN_NODE_ID, shardResolver);
    }
    
    public ClusterTopology(final SortedMap<Integer, ClusterNodeInfo> nodes, final int leaderNodeId, final ClusterShardResolver shardResolver) {
        this.nodes = (nodes == null) ? Collections.emptySortedMap() : Collections.unmodifiableSortedMap(nodes);
        this.leaderNodeId = leaderNodeId;
        this.shardResolver = shardResolver;
    }
    
    public SortedMap<Integer, ClusterNodeInfo> getNodes() {
        return nodes;
    }
    
    public int getLeaderNodeId() {
        return leaderNodeId;
    }
    
    public ClusterShardResolver getShardResolver() {
        return shardResolver;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(
            this, o, other -> (leaderNodeId == other.leaderNodeId) && Objects.equals(nodes, other.nodes));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(leaderNodeId, nodes);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("leaderNodeId", leaderNodeId).with("nodes", nodes).toString();
    }
    
}
