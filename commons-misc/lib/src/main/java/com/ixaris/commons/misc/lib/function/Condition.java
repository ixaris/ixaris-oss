package com.ixaris.commons.misc.lib.function;

@FunctionalInterface
public interface Condition<T> {
    
    boolean isTrue(T t);
    
}
