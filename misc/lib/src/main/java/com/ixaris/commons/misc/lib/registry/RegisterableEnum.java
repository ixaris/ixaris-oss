package com.ixaris.commons.misc.lib.registry;

/**
 * This interface should be implemented by a bean that wants to register enum values. the typical way to implement this is as follows
 *
 * <pre>
 * public enum Whatever implements WhateverRegisterable {
 *
 *     ...;
 *
 *     &#064;Override
 *     public String getKey() {
 *         return name();
 *     }
 *
 *     public static class WhateverEnum implements RegisterableEnum {
 *
 *         &#064;Override
 *         public Whatever[] getEnumValues() {
 *             return Whatever.values();
 *         }
 *     }
 *
 * }
 * </pre>
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public interface RegisterableEnum {
    
    Registerable[] getEnumValues();
    
}
