package com.ixaris.commons.misc.lib.object;

import java.util.Objects;

public final class Reference<T> {
    
    public static final class Integer {
        
        private int value;
        
        public Integer() {}
        
        public Integer(final int value) {
            this.value = value;
        }
        
        public int get() {
            return value;
        }
        
        public void set(final int value) {
            this.value = value;
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, 0, other -> value == other.value);
        }
        
        @Override
        public int hashCode() {
            return java.lang.Integer.hashCode(value);
        }
        
    }
    
    public static final class Boolean {
        
        private boolean value;
        
        public Boolean() {}
        
        public Boolean(final boolean value) {
            this.value = value;
        }
        
        public boolean get() {
            return value;
        }
        
        public void set(final boolean value) {
            this.value = value;
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, 0, other -> value == other.value);
        }
        
        @Override
        public int hashCode() {
            return java.lang.Boolean.hashCode(value);
        }
        
    }
    
    private T value;
    
    public Reference() {}
    
    public Reference(final T value) {
        this.value = value;
    }
    
    public T get() {
        return value;
    }
    
    public void set(final T value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, 0, other -> Objects.equals(value, other.value));
    }
    
    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
    
}
