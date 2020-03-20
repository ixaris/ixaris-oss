package com.ixaris.commons.misc.lib.registry;

public final class TestTypeRegistry extends Registry<TestType> {
    
    private static final TestTypeRegistry INSTANCE = new TestTypeRegistry();
    
    public static TestTypeRegistry getInstance() {
        return INSTANCE;
    }
    
    private TestTypeRegistry() {
        super();
    }
    
}
