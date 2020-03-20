package org.apache.logging.log4j.spi;

public final class Helper {
    
    public static CleanableThreadContextMap create() {
        return new GarbageFreeSortedArrayThreadContextMap();
    }
    
    private Helper() {}
    
}
