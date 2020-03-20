package com.ixaris.commons.misc.lib.registry;

/**
 * Interface to be implemented by items that can be registered with a registry. This will allow reverse resolution from a string to the
 * registered item.
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public interface Registerable {
    
    String getKey();
    
}
