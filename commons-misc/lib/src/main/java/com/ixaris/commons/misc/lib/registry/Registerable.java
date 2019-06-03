package com.ixaris.commons.misc.lib.registry;

/**
 * Interface to be implemented by items that can be registered with a registry. This will allow reverse resolution from
 * a string to the registered item.
 */
public interface Registerable {
    
    String getKey();
    
}
