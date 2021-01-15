package com.ixaris.commons.dimensions.limits;

import com.ixaris.commons.dimensions.limits.data.LimitEntity;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

public final class LimitExceeded {
    
    private final LimitEntity<?, ?> failedLimit;
    private final boolean countExceeded;
    private final boolean minLimitExceeded;
    private final boolean maxLimitExceeded;
    
    public LimitExceeded(final LimitEntity<?, ?> failedLimit,
                         final boolean countExceeded,
                         final boolean minLimitExceeded,
                         final boolean maxLimitExceeded) {
        this.failedLimit = failedLimit;
        this.countExceeded = countExceeded;
        this.minLimitExceeded = minLimitExceeded;
        this.maxLimitExceeded = maxLimitExceeded;
    }
    
    public LimitEntity<?, ?> getFailedLimit() {
        return failedLimit;
    }
    
    public boolean isCountExceeded() {
        return countExceeded;
    }
    
    public boolean isMinLimitExceeded() {
        return minLimitExceeded;
    }
    
    public boolean isMaxLimitExceeded() {
        return maxLimitExceeded;
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this)
            .with("failedLimit.id", failedLimit.getId())
            .with("minLimitExceeded", minLimitExceeded)
            .with("maxLimitExceeded", maxLimitExceeded)
            .with("countExceeded", countExceeded)
            .toString();
    }
}
