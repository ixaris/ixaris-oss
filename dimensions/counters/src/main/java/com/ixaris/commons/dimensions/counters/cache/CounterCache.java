package com.ixaris.commons.dimensions.counters.cache;

import org.jooq.UpdatableRecord;

import com.ixaris.commons.dimensions.counters.CounterDef;
import com.ixaris.commons.dimensions.counters.WindowWidth;
import com.ixaris.commons.dimensions.counters.data.CounterEntity;
import com.ixaris.commons.dimensions.lib.context.Context;

public interface CounterCache<R extends UpdatableRecord<R>, C extends CounterDef<R, C>> {
    
    CounterEntity<R, C> get(Context<C> context, WindowWidth narrowWindowWidth, int wideWindowMultiple);
    
    void put(CounterEntity<R, C> value);
    
    void invalidate(Context<C> context, WindowWidth narrowWindowWidth, int wideWindowMultiple);
    
    void clear();
    
}
