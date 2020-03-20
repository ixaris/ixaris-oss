package com.ixaris.commons.clustering.lib.service;

import java.util.Map;

import com.ixaris.commons.collections.lib.BitSet;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

public final class ShardRebalance {
    
    private final Map<Integer, BitSet> toStop;
    private final Map<Integer, BitSet> toStart;
    
    public ShardRebalance(final Map<Integer, BitSet> toStop, final Map<Integer, BitSet> toStart) {
        this.toStop = toStop;
        this.toStart = toStart;
    }
    
    public Map<Integer, BitSet> getToStop() {
        return toStop;
    }
    
    public Map<Integer, BitSet> getToStart() {
        return toStart;
    }
    
    public boolean isEmpty() {
        return toStop.isEmpty() && toStart.isEmpty();
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("toStop", toStop).with("toStart", toStart).toString();
    }
    
}
