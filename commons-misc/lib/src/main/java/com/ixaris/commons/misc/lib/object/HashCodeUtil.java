package com.ixaris.commons.misc.lib.object;

import java.util.Arrays;

/**
 * Collected methods which allow easy implementation of <code>hashCode</code>.
 *
 * <p>Example use case:
 *
 * <pre>
 * public int hashCode() {
 *     return HashCodeUtil.with(fPrimitive).with(fObject).with(fArray).get();
 * }
 * </pre>
 */
public final class HashCodeUtil {
    
    public static int hash(final boolean attribute) {
        return Boolean.hashCode(attribute);
    }
    
    public static int hash(final char attribute) {
        return Character.hashCode(attribute);
    }
    
    public static int hash(final int attribute) {
        return Integer.hashCode(attribute);
    }
    
    public static int hash(final long attribute) {
        return Long.hashCode(attribute);
    }
    
    public static int hash(final float attribute) {
        return Float.hashCode(attribute);
    }
    
    public static int hash(final double attribute) {
        return Double.hashCode(attribute);
    }
    
    public static int hash(final Object attribute) {
        if (attribute == null) {
            return 0;
        } else if (attribute.getClass().isArray()) {
            if (attribute instanceof byte[]) {
                return Arrays.hashCode((byte[]) attribute);
            } else if (attribute instanceof short[]) {
                return Arrays.hashCode((short[]) attribute);
            } else if (attribute instanceof int[]) {
                return Arrays.hashCode((int[]) attribute);
            } else if (attribute instanceof long[]) {
                return Arrays.hashCode((long[]) attribute);
            } else if (attribute instanceof char[]) {
                return Arrays.hashCode((char[]) attribute);
            } else if (attribute instanceof float[]) {
                return Arrays.hashCode((float[]) attribute);
            } else if (attribute instanceof double[]) {
                return Arrays.hashCode((double[]) attribute);
            } else if (attribute instanceof boolean[]) {
                return Arrays.hashCode((boolean[]) attribute);
            } else {
                return Arrays.deepHashCode((Object[]) attribute);
            }
        } else {
            return attribute.hashCode();
        }
    }
    
    public static Builder with(final boolean attribute) {
        return new Builder().with(attribute);
    }
    
    public static Builder with(final char attribute) {
        return new Builder().with(attribute);
    }
    
    public static Builder with(final int attribute) {
        return new Builder().with(attribute);
    }
    
    public static Builder with(final long attribute) {
        return new Builder().with(attribute);
    }
    
    public static Builder with(final float attribute) {
        return new Builder().with(attribute);
    }
    
    public static Builder with(final double attribute) {
        return new Builder().with(attribute);
    }
    
    public static Builder with(final Object attribute) {
        return new Builder().with(attribute);
    }
    
    public static final class Builder {
        
        private int result = 1;
        
        private Builder() {}
        
        public Builder with(final int attribute) {
            result = (31 * result) + attribute;
            return this;
        }
        
        public Builder with(final boolean attribute) {
            return with(hash(attribute));
        }
        
        public Builder with(final char attribute) {
            return with(hash(attribute));
        }
        
        public Builder with(final long attribute) {
            return with(hash(attribute));
        }
        
        public Builder with(final float attribute) {
            return with(hash(attribute));
        }
        
        public Builder with(final double attribute) {
            return with(hash(attribute));
        }
        
        public Builder with(final Object attribute) {
            return with(hash(attribute));
        }
        
        public int get() {
            return result;
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, o, other -> result == other.result);
        }
        
        @Override
        public int hashCode() {
            return result;
        }
        
    }
    
    private HashCodeUtil() {}
    
}
