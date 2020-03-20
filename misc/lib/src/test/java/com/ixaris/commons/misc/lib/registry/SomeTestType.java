package com.ixaris.commons.misc.lib.registry;

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
    
}
