package com.ixaris.commons.microservices.lib.client.support;

import java.util.Set;

@FunctionalInterface
public interface ServiceKeysResolver {
    
    Set<String> getKeys();
    
}
