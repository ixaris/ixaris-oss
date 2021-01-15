package com.ixaris.commons.dimensions.limits.cache;

import com.ixaris.commons.dimensions.limits.LimitDef;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;

public interface LimitsCache<I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> {
    
    LimitCacheEntry<I, L> get();
    
    void set(LimitCacheEntry<I, L> entry);
    
    void clear();
    
}
