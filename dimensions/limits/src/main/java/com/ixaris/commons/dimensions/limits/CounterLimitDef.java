/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits;

import java.util.Set;

import com.ixaris.commons.dimensions.counters.CounterDef;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;

public interface CounterLimitDef<I extends AbstractLimitExtensionEntity<?, I>, C extends CounterDef<?, C>> extends LimitDef<I> {
    
    C getCounterDef();
    
    /**
     * Retrieve a list of ContextDimensions which have an unchanging value regardless of the configuration of this LimitDef. Example use case:
     * the LimitDef depends on a counter (like Windowed limits). These dimensions will be used in combination with the counter, to retrieve the
     * correct counter values. The AML limit works in this way - we require only funds with polarity IN to be counted (and it does not make sense
     * to configure the limit with only 1 value).
     *
     * @return a set of ContextDimensions which are not configurable, but required for this LimitDef to work correctly.
     */
    Set<Dimension<?>> getConstantDimensions();
    
    CommonsDimensionsLimits.CounterLimitDef toProtobuf();
    
}
