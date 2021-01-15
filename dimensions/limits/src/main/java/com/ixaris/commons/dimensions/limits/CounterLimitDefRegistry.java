/*
 * Copyright 2002, 2009 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits;

import org.springframework.stereotype.Component;

import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.CounterLimitDefs;
import com.ixaris.commons.misc.lib.registry.Registry;
import com.ixaris.commons.misc.spring.AbstractSingletonFactoryBean;

public final class CounterLimitDefRegistry extends Registry<CounterLimitDef<?, ?>> {
    
    private static final CounterLimitDefRegistry INSTANCE = new CounterLimitDefRegistry();
    
    public static CounterLimitDefRegistry getInstance() {
        return INSTANCE;
    }
    
    @Component
    public static final class CounterLimitDefRegistryBean extends AbstractSingletonFactoryBean<CounterLimitDefRegistry> {}
    
    private CounterLimitDefRegistry() {
        super(1, 50);
    }
    
    public CounterLimitDefs toProtobuf() {
        final CounterLimitDefs.Builder builder = CounterLimitDefs.newBuilder();
        for (final CounterLimitDef<?, ?> limitDef : getRegisteredValues()) {
            builder.addLimitDefs(limitDef.toProtobuf());
        }
        return builder.build();
    }
    
}
