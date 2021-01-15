package com.ixaris.commons.netty.clustering;

import static com.ixaris.commons.clustering.lib.common.ClusterTopology.UNKNOWN_NODE_ID;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import com.ixaris.commons.clustering.lib.common.ClusterShardResolver;
import com.ixaris.commons.netty.clustering.NettyBean.HostAndPort;

import io.netty.channel.Channel;

public final class NettyCluster {
    
    private final int leaderNodeId;
    private final ImmutableMap<HostAndPort, NettyClientChannel> nodes;
    private final Map<Integer, NettyClientChannel> shards;
    private final NettyClientChannel leader;
    private final ClusterShardResolver shardResolver;
    
    public NettyCluster(final ClusterShardResolver shardResolver) {
        this(UNKNOWN_NODE_ID, ImmutableMap.of(), Collections.emptyMap(), null, shardResolver);
    }
    
    public NettyCluster(final int leaderNodeId,
                        final ImmutableMap<HostAndPort, NettyClientChannel> nodes,
                        final Map<Integer, NettyClientChannel> shards,
                        final NettyClientChannel leader,
                        final ClusterShardResolver shardResolver) {
        this.leaderNodeId = leaderNodeId;
        this.nodes = nodes;
        this.shards = shards;
        this.leader = leader;
        this.shardResolver = shardResolver;
    }
    
    public int getLeaderNodeId() {
        return leaderNodeId;
    }
    
    public ImmutableMap<HostAndPort, NettyClientChannel> getNodes() {
        return nodes;
    }
    
    public Channel getChannelForShard(final int shard) {
        return Optional.ofNullable(shards.get(shard)).map(NettyClientChannel::getChannel).orElse(null);
    }
    
    public Channel getLeaderChannel() {
        return (leader != null) ? leader.getChannel() : null;
    }
    
    public ClusterShardResolver getShardResolver() {
        return shardResolver;
    }
    
}
