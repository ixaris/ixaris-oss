package com.ixaris.commons.misc.lib.registry;

public enum SomeTestEnum implements TestType {
    
    TEST2,
    TEST3;
    
    @Override
    public String getKey() {
        return name();
    }
    
    public static final class SomeTestEnumContainer implements RegisterableEnum {
        
        public static final SomeTestEnumContainer INSTANCE = new SomeTestEnumContainer();
        
        @Override
        public SomeTestEnum[] getEnumValues() {
            return SomeTestEnum.values();
        }
        
    }
    
}
