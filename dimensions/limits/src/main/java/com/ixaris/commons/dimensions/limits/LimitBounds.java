/*
 * Copyright 2002, 2015 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits;

import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;
import com.ixaris.commons.dimensions.limits.data.LimitEntity;

public class LimitBounds<I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> {
    
    private final LimitEntity<I, L> countLimit;
    private final Long count;
    
    private final LimitEntity<I, L> minLimit;
    private final Long min;
    
    private final LimitEntity<I, L> maxLimit;
    private final Long max;
    
    public LimitBounds(final LimitEntity<I, L> countLimit,
                       final Long count,
                       final LimitEntity<I, L> minLimit,
                       final Long min,
                       final LimitEntity<I, L> maxLimit,
                       final Long max) {
        this.countLimit = countLimit;
        this.count = count;
        this.minLimit = minLimit;
        this.min = min;
        this.maxLimit = maxLimit;
        this.max = max;
    }
    
    public LimitEntity<I, L> getCountLimit() {
        return countLimit;
    }
    
    public Long getCount() {
        return count;
    }
    
    public LimitEntity<I, L> getMinLimit() {
        return minLimit;
    }
    
    public Long getMin() {
        return min;
    }
    
    public LimitEntity<I, L> getMaxLimit() {
        return maxLimit;
    }
    
    public Long getMax() {
        return max;
    }
    
}
