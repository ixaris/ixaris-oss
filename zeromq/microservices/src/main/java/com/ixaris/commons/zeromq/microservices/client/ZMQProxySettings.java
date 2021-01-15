package com.ixaris.commons.zeromq.microservices.client;

import java.util.Set;

public final class ZMQProxySettings {
    
    public enum Mode {
        
        PROXY_ONLY,
        PROXY_ALL_EXCEPT
        
    }
    
    public ZMQProxySettings(final String proxyCluster, final Set<String> proxyFor, final Mode mode) {
        this.proxyCluster = proxyCluster;
        this.proxyFor = proxyFor;
        this.mode = mode;
    }
    
    private final String proxyCluster;
    private final Set<String> proxyFor;
    private final Mode mode;
    
    public String getProxyCluster() {
        return proxyCluster;
    }
    
    public boolean isProxied(final String clusterName) {
        switch (mode) {
            case PROXY_ONLY:
                return proxyFor.contains(clusterName);
            case PROXY_ALL_EXCEPT:
                return !proxyFor.contains(clusterName);
            default:
                throw new UnsupportedOperationException(mode.name());
        }
    }
    
}
