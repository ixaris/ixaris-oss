package com.ixaris.commons.misc.spring.singleton;

import org.springframework.stereotype.Component;

import com.ixaris.commons.misc.spring.AbstractSingletonFactoryBean;

public final class SomeTestType implements TestType {
    
    public static final String KEY = "TEST1";
    
    private static final SomeTestType INSTANCE = new SomeTestType();
    
    public static SomeTestType getInstance() {
        return INSTANCE;
    }
    
    private SomeTestType() {}
    
    @Override
    public String getKey() {
        return KEY;
    }
    
    @Component
    public static final class SomeTestTypeFactoryBean extends AbstractSingletonFactoryBean<SomeTestType> {}
    
}
