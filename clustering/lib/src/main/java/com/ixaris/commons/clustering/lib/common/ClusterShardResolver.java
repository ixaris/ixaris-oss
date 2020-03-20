package com.ixaris.commons.clustering.lib.common;

public interface ClusterShardResolver {
    
    static long getShardKeyFromString(final String key) {
        if (key.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(key);
        } catch (final NumberFormatException e) {
            return key.hashCode();
        }
    }
    
    ClusterShardResolver DEFAULT = new ClusterShardResolver() {
        
        @Override
        public int getMaxShards() {
            return 1;
        }
        
        @Override
        public int getShard(long id) {
            return 0;
        }
    };
    
    int getMaxShards();
    
    /**
     * Determine the shard for a given shard key
     */
    int getShard(long id);
    
}
