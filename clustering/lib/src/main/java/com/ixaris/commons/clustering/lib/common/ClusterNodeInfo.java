package com.ixaris.commons.clustering.lib.common;

import java.util.Objects;

import com.google.common.collect.ImmutableMap;

import com.ixaris.commons.collections.lib.BitSet;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

public final class ClusterNodeInfo {
    
    private final long timestamp;
    private final BitSet shards;
    private final BitSet shardsStopping;
    private final ImmutableMap<String, String> attributes;
    
    public ClusterNodeInfo(final long timestamp,
                           final BitSet shards,
                           final BitSet shardsStopping,
                           final ImmutableMap<String, String> attributes) {
        this.timestamp = timestamp;
        this.shards = (shards == null) ? BitSet.empty() : BitSet.unmodifiable(shards);
        this.shardsStopping = (shardsStopping == null) ? BitSet.empty() : BitSet.unmodifiable(shardsStopping);
        this.attributes = attributes == null ? ImmutableMap.of() : attributes;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public BitSet getShards() {
        return shards;
    }
    
    public BitSet getShardsStopping() {
        return shardsStopping;
    }
    
    public ImmutableMap<String, String> getAttributes() {
        return attributes;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> (timestamp == other.timestamp)
            && shards.equals(other.shards)
            && shardsStopping.equals(other.shardsStopping)
            && attributes.equals(other.attributes));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(timestamp, shards, shardsStopping, attributes);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this)
            .with("timestamp", timestamp)
            .with("shards", shards.size())
            .with("shardsStopping", shardsStopping.size())
            .with("attributes", attributes)
            .toString();
    }
    
}
