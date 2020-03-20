package com.ixaris.commons.microservices.lib.client.proxy;

import java.util.Objects;

import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.misc.lib.object.EqualsUtil;

public final class ListenerKey {
    
    private final String listenerName;
    private final ServicePathHolder path;
    
    public ListenerKey(final String listenerName, final ServicePathHolder path) {
        this.listenerName = listenerName;
        this.path = path;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(
            this, o, other -> listenerName.equals(other.listenerName) && path.equals(other.path));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(listenerName, path);
    }
    
}
