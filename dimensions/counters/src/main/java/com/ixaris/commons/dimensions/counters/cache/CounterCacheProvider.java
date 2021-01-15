package com.ixaris.commons.dimensions.counters.cache;

import org.jooq.UpdatableRecord;

import com.ixaris.commons.dimensions.counters.CounterDef;

public interface CounterCacheProvider {
    
    <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> CounterCache<R, C> of(C def);
    
}
