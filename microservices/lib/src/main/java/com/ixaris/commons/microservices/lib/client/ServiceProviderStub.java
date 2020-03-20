package com.ixaris.commons.microservices.lib.client;

import java.util.Set;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public interface ServiceProviderStub extends ServiceStub {
    
    String KEYS_METHOD_NAME = "_keys";
    
    /**
     * Method name starts with _ to avoid collisions with resource methods
     *
     * <p>Implementations of this interface should cache the set, therefore consumers of this interface should not cache this set.
     *
     * @return the currently available keys for a service provider
     */
    Set<String> _keys();
    
}
