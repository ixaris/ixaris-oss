package com.ixaris.commons.dimensions.config.cache;

import com.ixaris.commons.dimensions.config.ConfigDef;
import com.ixaris.commons.dimensions.lib.context.Context;

public interface ConfigCache {
    
    Object get(Context<? extends ConfigDef<?>> key);
    
    void put(Context<? extends ConfigDef<?>> key, Object value);
    
    /**
     * for cluster caches, should invalidate on all nodes
     */
    void invalidate();
    
}
