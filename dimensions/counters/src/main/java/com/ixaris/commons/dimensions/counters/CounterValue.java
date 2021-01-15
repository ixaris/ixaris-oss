package com.ixaris.commons.dimensions.counters;

import java.util.Objects;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

public final class CounterValue {
    
    private final WindowValue wide;
    private final WindowValue narrow;
    
    public CounterValue(final WindowValue wide, final WindowValue narrow) {
        this.wide = wide;
        this.narrow = narrow;
    }
    
    public WindowValue getWide() {
        return wide;
    }
    
    public WindowValue getNarrow() {
        return narrow;
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("wide", wide).with("narrow", narrow).toString();
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> wide.equals(other.wide) && narrow.equals(other.narrow));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(wide, narrow);
    }
    
}
