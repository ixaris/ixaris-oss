/*
 * Copyright 2002, 2009 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters;

import org.springframework.stereotype.Component;

import com.ixaris.commons.misc.lib.registry.Registry;
import com.ixaris.commons.misc.spring.AbstractSingletonFactoryBean;

/**
 * Property Definition Registry
 *
 * @author <a href="brian.vella@ixaris.com">Brian Vella</a>
 */
public final class CounterDefRegistry extends Registry<CounterDef<?, ?>> {
    
    private static final CounterDefRegistry INSTANCE = new CounterDefRegistry();
    
    public static CounterDefRegistry getInstance() {
        return INSTANCE;
    }
    
    @Component
    public static final class CounterDefRegistryBean extends AbstractSingletonFactoryBean<CounterDefRegistry> {}
    
    private CounterDefRegistry() {
        super(1, 50);
    }
    
}
