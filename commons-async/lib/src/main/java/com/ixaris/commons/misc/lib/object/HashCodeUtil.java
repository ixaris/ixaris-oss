package com.ixaris.commons.misc.lib.object;

import java.util.Arrays;

/**
 * Collected methods which allow easy implementation of <code>hashCode</code>.
 * 
 * Example use case:
 * 
 * <pre>
 * public int hashCode() {
 *     return HashCodeUtil.seed(HashCodeUtil.SEED_13).with(fPrimitive).with(fObject).with(fArray);
 * }
 * </pre>
 */
public final class HashCodeUtil {
    
    private static final int PRIME_NUMBER = 37;
    
    public static final int SEED_7 = 7;
    public static final int SEED_13 = 13;
    public static final int SEED_17 = 17;
    public static final int SEED_23 = 23;
    public static final int SEED_31 = 31;
    
    public static HashCodeUtil seed(final int seed) {
        return new HashCodeUtil(seed);
    }
    
    private int result;
    
    private HashCodeUtil(final int seed) {
        result = seed;
    }
    
    /**
     * @param attribute
     */
    public HashCodeUtil with(final boolean attribute) {
        result = (PRIME_NUMBER * result) + hash(attribute);
        return this;
    }
    
    /**
     * @param attribute
     */
    public HashCodeUtil with(final char attribute) {
        result = (PRIME_NUMBER * result) + hash(attribute);
        return this;
    }
    
    /**
     * @param attribute
     */
    public HashCodeUtil with(final int attribute) {
        result = (PRIME_NUMBER * result) + hash(attribute);
        return this;
    }
    
    /**
     * @param attribute
     */
    public HashCodeUtil with(final long attribute) {
        result = (PRIME_NUMBER * result) + hash(attribute);
        return this;
    }
    
    /**
     * @param attribute
     */
    public HashCodeUtil with(final float attribute) {
        return with(hash(attribute));
    }
    
    /**
     * @param attribute
     */
    public HashCodeUtil with(final double attribute) {
        return with(hash(attribute));
    }
    
    /**
     * @param attribute which is either null, an array or an object
     * @return the updated hashcode
     */
    public HashCodeUtil with(final Object attribute) {
        return with(hash(attribute));
    }
    
    public int hashCode() { // NOSONAR utility to calculate hashcode, does not need equals
        return result;
    }
    
    /**
     * @param attribute
     */
    public static int hash(final boolean attribute) {
        return attribute ? 1231 : 1237; // same as Boolean.hashCode();
    }
    
    /**
     * @param attribute
     */
    public static int hash(final char attribute) {
        return (int) attribute; // same as Character.hashCode()
    }
    
    /**
     * @param attribute
     */
    public static int hash(final int attribute) {
        return attribute; // same as Integer.hashCode()
    }
    
    /**
     * @param attribute
     */
    public static int hash(final long attribute) {
        return (int) (attribute ^ (attribute >>> 32)); // same as Long.hashCode();
    }
    
    /**
     * @param attribute
     */
    public static int hash(final float attribute) {
        return Float.floatToIntBits(attribute); // same as Float.hashCode();
    }
    
    /**
     * @param attribute
     */
    public static int hash(final double attribute) {
        return hash(Double.doubleToLongBits(attribute)); // same as Double.hashCode();
    }
    
    /**
     * @param result hashcode so far
     * @param attribute which is either null, an array or an object
     * @return the updated hashcode
     */
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
    
}
