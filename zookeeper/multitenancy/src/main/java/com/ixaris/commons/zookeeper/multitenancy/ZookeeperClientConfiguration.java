package com.ixaris.commons.zookeeper.multitenancy;

/**
 * This class holds all configuration that is needed to connect to Zookeeper Service Discovery.
 *
 * @author aldrin.seychell
 */
public class ZookeeperClientConfiguration {
    
    private static final int DEFAULT_ZK_TIMEOUT = 20000;
    
    private final String zookeeperUrl;
    private int sessionTimeout = DEFAULT_ZK_TIMEOUT;
    private int connectionTimeout = DEFAULT_ZK_TIMEOUT;
    private int maximumRetries = 20;
    private int maximumSleepTimeMs = 60000;
    private int baseSleepTimeMs = 100;
    
    public ZookeeperClientConfiguration(final String zookeeperUrl) {
        this.zookeeperUrl = zookeeperUrl;
    }
    
    public String getZookeeperUrl() {
        return zookeeperUrl;
    }
    
    public int getSessionTimeout() {
        return sessionTimeout;
    }
    
    public void setSessionTimeout(final int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getMaximumRetries() {
        return maximumRetries;
    }
    
    public void setMaximumRetries(final int maximumRetries) {
        this.maximumRetries = maximumRetries;
    }
    
    public int getMaximumSleepTimeMs() {
        return maximumSleepTimeMs;
    }
    
    public void setMaximumSleepTimeMs(final int maximumSleepTimeMs) {
        this.maximumSleepTimeMs = maximumSleepTimeMs;
    }
    
    public int getBaseSleepTimeMs() {
        return baseSleepTimeMs;
    }
    
    public void setBaseSleepTimeMs(final int baseSleepTimeMs) {
        this.baseSleepTimeMs = baseSleepTimeMs;
    }
}
