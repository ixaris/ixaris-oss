package com.ixaris.commons.misc.lib.registry;

@Archived
public class ArchivedTestType implements TestType {
    
    public static final String KEY = "ARCHIVED";
    
    private static final ArchivedTestType INSTANCE = new ArchivedTestType();
    
    public static ArchivedTestType getInstance() {
        return INSTANCE;
    }
    
    private ArchivedTestType() {}
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
