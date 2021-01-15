/*
 * Copyright 2002, 2009 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits;

import org.springframework.stereotype.Component;

import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.ValueLimitDefs;
import com.ixaris.commons.misc.lib.registry.Registry;
import com.ixaris.commons.misc.spring.AbstractSingletonFactoryBean;

public final class ValueLimitDefRegistry extends Registry<ValueLimitDef<?>> {
    
    private static final ValueLimitDefRegistry INSTANCE = new ValueLimitDefRegistry();
    
    public static ValueLimitDefRegistry getInstance() {
        return INSTANCE;
    }
    
    @Component
    public static final class ValueLimitDefRegistryBean extends AbstractSingletonFactoryBean<ValueLimitDefRegistry> {}
    
    private ValueLimitDefRegistry() {
        super(1, 50);
    }
    
    public ValueLimitDefs toProtobuf() {
        final ValueLimitDefs.Builder builder = ValueLimitDefs.newBuilder();
        for (final ValueLimitDef<?> limitDef : getRegisteredValues()) {
            builder.addLimitDefs(limitDef.toProtobuf());
        }
        return builder.build();
    }
    
}
