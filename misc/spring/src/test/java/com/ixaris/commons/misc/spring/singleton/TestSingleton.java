package com.ixaris.commons.misc.spring.singleton;

import org.springframework.stereotype.Component;

import com.ixaris.commons.misc.spring.AbstractSingletonFactoryBean;

public class TestSingleton {
    
    private static final TestSingleton INSTANCE = new TestSingleton();
    
    public static TestSingleton getInstance() {
        return INSTANCE;
    }
    
    private TestSingleton() {}
    
    @Component
    public static final class TestSingletonFactoryBean extends AbstractSingletonFactoryBean<TestSingleton> {}
    
}
