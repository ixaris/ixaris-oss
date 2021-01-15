/*
 * Copyright 2002, 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters;

import java.util.Objects;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

/**
 * Groups a window count & sum values. This class is immutable.
 *
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public final class WindowValue {
    
    private long count;
    private long sum;
    
    public WindowValue(long count, long sum) {
        this.count = count;
        this.sum = sum;
    }
    
    public long getCount() {
        return count;
    }
    
    public long getSum() {
        return sum;
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("count", count).with("sum", sum).toString();
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> (count == other.count) && (sum == other.sum));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(count, sum);
    }
    
}
