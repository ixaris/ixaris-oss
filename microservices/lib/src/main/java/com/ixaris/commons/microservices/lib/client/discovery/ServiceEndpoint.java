package com.ixaris.commons.microservices.lib.client.discovery;

import java.util.Objects;

import com.google.common.collect.ImmutableMap;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * An immutable representation of an endpoint topology
 *
 * @author aldrin.seychell
 */
public final class ServiceEndpoint {
    
    private final String name;
    private final ImmutableMap<String, String> serviceKeys;
    
    public ServiceEndpoint(final String name, final ImmutableMap<String, String> serviceKeys) {
        
        this.name = name;
        this.serviceKeys = serviceKeys == null ? ImmutableMap.of() : serviceKeys;
    }
    
    public String getName() {
        return name;
    }
    
    public ImmutableMap<String, String> getServiceKeys() {
        return serviceKeys;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(
            this, o, other -> Objects.equals(name, other.name) && Objects.equals(serviceKeys, other.serviceKeys));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, serviceKeys);
    }
    
    @Override
    public String toString() {
        return "Topology for [" + name + "]: " + serviceKeys;
    }
    
}
