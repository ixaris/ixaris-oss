package com.ixaris.commons.clustering.lib.common;

import java.util.Objects;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

public final class ModClusterShardResolver implements ClusterShardResolver {
    
    public static int getShard(final long id, final int maxShards) {
        // java % can produce negative numbers. floorMod returns numbers 0 to n-1
        // https://stackoverflow.com/questions/4412179/best-way-to-make-javas-modulus-behave-like-it-should-with-negative-numbers
        return (int) Math.floorMod(id, maxShards);
    }
    
    private final int maxShards;
    
    public ModClusterShardResolver(final int maxShards) {
        if (maxShards < 1) {
            throw new IllegalArgumentException();
        }
        this.maxShards = maxShards;
    }
    
    @Override
    public int getMaxShards() {
        return maxShards;
    }
    
    @Override
    public int getShard(final long id) {
        return getShard(id, maxShards);
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> maxShards == other.maxShards);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(maxShards);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("maxShards", maxShards).toString();
    }
    
}
