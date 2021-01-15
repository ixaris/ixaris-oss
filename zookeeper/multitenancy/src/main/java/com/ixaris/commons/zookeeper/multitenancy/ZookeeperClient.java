package com.ixaris.commons.zookeeper.multitenancy;

import java.util.ArrayList;
import java.util.List;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.utils.ZKPaths;

public final class ZookeeperClient {
    
    private static final byte[] DEFAULT_DATA_IN_ZK = {};
    
    private final CuratorFramework client;
    private final List<Runnable> preCloseTasks = new ArrayList<>();
    
    public ZookeeperClient(final ZookeeperClientConfiguration cfg) {
        final RetryPolicy retryPolicy = new BoundedExponentialBackoffRetry(
            cfg.getBaseSleepTimeMs(), cfg.getMaximumSleepTimeMs(), cfg.getMaximumRetries());
        
        final String connectString = cfg.getZookeeperUrl();
        if (connectString.contains("/")) {
            final int i = connectString.indexOf('/');
            final String root = connectString.substring(0, i);
            final String chroot = connectString.substring(i);
            
            CuratorFramework tmpClient = CuratorFrameworkFactory.builder()
                .connectString(root)
                .retryPolicy(retryPolicy)
                .sessionTimeoutMs(cfg.getSessionTimeout())
                .connectionTimeoutMs(cfg.getConnectionTimeout())
                .build();
            
            tmpClient.start();
            try {
                ZKPaths.mkdirs(tmpClient.getZookeeperClient().getZooKeeper(), chroot);
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot ensure chroot path", exception);
            } finally {
                tmpClient.close();
            }
        }
        
        this.client = CuratorFrameworkFactory.builder()
            .connectString(connectString)
            .retryPolicy(retryPolicy)
            .defaultData(DEFAULT_DATA_IN_ZK)
            .sessionTimeoutMs(cfg.getSessionTimeout())
            .connectionTimeoutMs(cfg.getConnectionTimeout())
            .build();
        
        client.start();
        try {
            client.getZookeeperClient().blockUntilConnectedOrTimedOut();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
    
    public CuratorFramework get() {
        return client;
    }
    
    public void close() {
        synchronized (preCloseTasks) {
            for (final Runnable task : preCloseTasks) {
                task.run();
            }
            CloseableUtils.closeQuietly(client);
        }
    }
    
    public void addCloseTask(final Runnable task) {
        synchronized (preCloseTasks) {
            preCloseTasks.add(task);
        }
    }
    
}
