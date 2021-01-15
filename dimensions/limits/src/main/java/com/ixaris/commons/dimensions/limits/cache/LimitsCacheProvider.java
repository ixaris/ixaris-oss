package com.ixaris.commons.dimensions.limits.cache;

import com.ixaris.commons.dimensions.limits.LimitDef;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;

public interface LimitsCacheProvider {
    
    <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> LimitsCache<I, L> of(L def);
    
}
