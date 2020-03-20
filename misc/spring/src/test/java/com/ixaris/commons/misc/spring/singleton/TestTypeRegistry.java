package com.ixaris.commons.misc.spring.singleton;

import org.springframework.stereotype.Component;

import com.ixaris.commons.misc.lib.registry.Registry;
import com.ixaris.commons.misc.spring.AbstractSingletonFactoryBean;

public final class TestTypeRegistry extends Registry<TestType> {
    
    private static final TestTypeRegistry INSTANCE = new TestTypeRegistry();
    
    public static TestTypeRegistry getInstance() {
        return INSTANCE;
    }
    
    private TestTypeRegistry() {
        super();
    }
    
    @Component
    public static final class TestTypeRegistryFactoryBean extends AbstractSingletonFactoryBean<TestTypeRegistry> {}
    
}
