/*
 * Copyright 2002, 2009 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config;

import org.springframework.stereotype.Component;

import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSetDefs;
import com.ixaris.commons.misc.lib.registry.Registry;
import com.ixaris.commons.misc.spring.AbstractSingletonFactoryBean;

/**
 * Set Definition Registry
 *
 * @author <a href="brian.vella@ixaris.com">Brian Vella</a>
 */
public final class SetDefRegistry extends Registry<SetDef<?>> {
    
    private static final SetDefRegistry INSTANCE = new SetDefRegistry();
    
    public static SetDefRegistry getInstance() {
        return INSTANCE;
    }
    
    @Component
    public static final class SetDefRegistryBean extends AbstractSingletonFactoryBean<SetDefRegistry> {}
    
    private SetDefRegistry() {
        super(1, 50);
    }
    
    public ConfigSetDefs toProtobuf() {
        final ConfigSetDefs.Builder builder = ConfigSetDefs.newBuilder();
        for (final SetDef<?> setDef : getRegisteredValues()) {
            builder.addSetDefs(setDef.toProtobuf());
        }
        return builder.build();
    }
    
}
