/*
 * Copyright 2002, 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters.data;

import com.ixaris.commons.dimensions.counters.CounterDef;
import com.ixaris.commons.dimensions.lib.context.Context;

/**
 * Every Counter Event which will update any matched context counter(s), must implement this interface. A mutable event may be passed several
 * times to the context counter module. Once the event becomes immutable, it should be passed to the module one last time. Whether to apply or
 * rollback the value from the counter, or ignore altogether is based on the values returned by the is...() methods.
 *
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public interface CounterEventEntity<C extends CounterDef<?, C>> {
    
    long getId();
    
    boolean isCounterAffected();
    
    /**
     * @return The context instance of the particular event. This should be the full context defined by the event. A number of counter contexts
     *     may match this context.
     */
    Context<C> getContext();
    
    /**
     * @return The amount to add to the matched counters.
     */
    long getDelta();
    
    /**
     * @return The timestamp of the event, to reflect which narrow window to increment.
     */
    long getTimestamp();
    
}
