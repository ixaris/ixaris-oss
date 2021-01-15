package com.ixaris.commons.zookeeper.multitenancy;

import static com.ixaris.commons.async.lib.Async.result;
import static org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode.POST_INITIALIZED_EVENT;
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_ADDED;
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED;

import java.io.IOException;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.clustering.TenantRegistry;

public final class TenantProvider implements TenantRegistry {
    
    private static final Logger LOG = LoggerFactory.getLogger(TenantProvider.class);
    
    private final MultiTenancy multiTenancy;
    private final ZookeeperClient client;
    private final String path;
    private final PathChildrenCacheListener listener = this::handleChildAdded;
    
    private PathChildrenCache childrenCache;
    
    public TenantProvider(final MultiTenancy multiTenancy, final ZookeeperClient client, final String path) {
        this.multiTenancy = multiTenancy;
        this.client = client;
        this.path = path;
    }
    
    @SuppressWarnings({ "squid:S00108", "squid:S1166", "checkstyle:com.puppycrawl.tools.checkstyle.checks.blocks.EmptyCatchBlockCheck" })
    public void start() {
        childrenCache = new PathChildrenCache(this.client.get(), path, true);
        childrenCache.getListenable().addListener(listener);
        try {
            childrenCache.start(POST_INITIALIZED_EVENT);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        
        client.addCloseTask(() -> {
            if (childrenCache != null) {
                childrenCache.getListenable().removeListener(listener);
                try {
                    childrenCache.close(); // best effort
                } catch (final IOException ignored) {}
            }
        });
    }
    
    @Override
    @SuppressWarnings("squid:S1166")
    public Async<Void> registerTenant(final String tenantId) {
        try {
            client.get().create().creatingParentsIfNeeded().forPath(ZKPaths.makePath(path, tenantId));
            return result();
        } catch (final NodeExistsException ignored) {
            return result();
        } catch (final Exception e) {
            LOG.warn("Error while registering tenant " + tenantId, e);
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    @SuppressWarnings("squid:S1166")
    public Async<Void> deregisterTenant(final String tenantId) {
        try {
            client.get().delete().forPath(ZKPaths.makePath(path, tenantId));
            return result();
        } catch (final NodeExistsException ignored) {
            return result();
        } catch (final Exception e) {
            LOG.warn("Error while deregistering tenant " + tenantId, e);
            throw new IllegalStateException(e);
        }
    }
    
    @SuppressWarnings("squid:S1172")
    private void handleChildAdded(final CuratorFramework client, final PathChildrenCacheEvent event) {
        if (event.getType() == CHILD_ADDED) {
            multiTenancy.addTenant(ZKPaths.getNodeFromPath(event.getData().getPath()));
        } else if (event.getType() == CHILD_REMOVED) {
            multiTenancy.removeTenant(ZKPaths.getNodeFromPath(event.getData().getPath()));
        }
    }
    
}
