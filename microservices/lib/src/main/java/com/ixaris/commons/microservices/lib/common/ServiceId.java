package com.ixaris.commons.microservices.lib.common;

import java.util.Objects;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.misc.lib.object.EqualsUtil;

public class ServiceId {
    
    private final String name;
    private final String key;
    
    public ServiceId(final RequestEnvelope request) {
        this(request.getServiceName(), request.getServiceKey());
    }
    
    public ServiceId(final String name, final String key) {
        this.name = name;
        this.key = key;
    }
    
    public String getName() {
        return name;
    }
    
    public String getKey() {
        return key;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, that -> Objects.equals(name, that.name) && Objects.equals(key, that.key));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, key);
    }
    
    @Override
    public String toString() {
        return name + "/" + key;
    }
    
}
