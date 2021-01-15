/*
 * Copyright 2002, 2009 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config;

import org.springframework.stereotype.Component;

import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValueDefs;
import com.ixaris.commons.misc.lib.registry.Registry;
import com.ixaris.commons.misc.spring.AbstractSingletonFactoryBean;

/**
 * Property Definition Registry
 *
 * @author <a href="brian.vella@ixaris.com">Brian Vella</a>
 */
public final class ValueDefRegistry extends Registry<ValueDef<?>> {
    
    private static final ValueDefRegistry INSTANCE = new ValueDefRegistry();
    
    public static ValueDefRegistry getInstance() {
        return INSTANCE;
    }
    
    @Component
    public static final class ValueDefRegistryBean extends AbstractSingletonFactoryBean<ValueDefRegistry> {}
    
    private ValueDefRegistry() {
        super(1, 50);
    }
    
    public ConfigValueDefs toProtobuf() {
        final ConfigValueDefs.Builder builder = ConfigValueDefs.newBuilder();
        for (final ValueDef<?> valueDef : getRegisteredValues()) {
            builder.addValueDefs(valueDef.toProtobuf());
        }
        return builder.build();
    }
    
}
