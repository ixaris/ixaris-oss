package com.ixaris.commons.zookeeper.clustering;

import org.apache.curator.framework.recipes.cache.ChildData;

public final class PathNodeCacheEvent {
    
    private final Type type;
    private final ChildData data;
    
    /**
     * Type of change
     */
    public enum Type {
        NODE_UPDATED,
        NODE_REMOVED
    }
    
    public PathNodeCacheEvent(final Type type, final ChildData data) {
        this.type = type;
        this.data = data;
    }
    
    public Type getType() {
        return type;
    }
    
    public ChildData getData() {
        return data;
    }
    
}
