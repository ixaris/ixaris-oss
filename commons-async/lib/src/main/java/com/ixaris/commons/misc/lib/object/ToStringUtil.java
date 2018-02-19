package com.ixaris.commons.misc.lib.object;

/**
 * To string utility to construct a string representation of an object.
 * NOT THREAD SAFE
 */
public final class ToStringUtil {
    
    public static ToStringUtil of(final Object o) {
        return new ToStringUtil(o);
    }
    
    private StringBuilder sb;
    private boolean first = true;
    private boolean finished = false;
    
    private ToStringUtil(final Object o) {
        sb = new StringBuilder(o.getClass().getSimpleName());
    }
    
    public ToStringUtil with(final String name, final boolean attribute) {
        return with(name, Boolean.toString(attribute));
    }
    
    public ToStringUtil with(final String name, final char attribute) {
        return with(name, Character.toString(attribute));
    }
    
    public ToStringUtil with(final String name, final int attribute) {
        return with(name, Integer.toString(attribute));
    }
    
    public ToStringUtil with(final String name, final long attribute) {
        return with(name, Long.toString(attribute));
    }
    
    public ToStringUtil with(final String name, final float attribute) {
        return with(name, Float.toString(attribute));
    }
    
    public ToStringUtil with(final String name, final double attribute) {
        return with(name, Double.toString(attribute));
    }
    
    public ToStringUtil with(final String name, final Object attribute) {
        if (finished) {
            throw new IllegalStateException("toString() already called.");
        }
        
        if (first) {
            first = false;
            sb.append(" {");
        } else {
            sb.append(", ");
        }
        sb.append(name).append(" = ").append(attribute);
        return this;
    }
    
    public String toString() {
        if (!finished) {
            finished = true;
            if (!first) {
                sb.append("}");
            }
        }
        return sb.toString();
    }
}
