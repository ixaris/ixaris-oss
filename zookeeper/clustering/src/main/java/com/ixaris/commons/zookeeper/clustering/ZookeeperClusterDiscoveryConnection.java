package com.ixaris.commons.zookeeper.clustering;

import static com.ixaris.commons.zookeeper.clustering.PathNodeCacheEvent.Type.NODE_REMOVED;
import static com.ixaris.commons.zookeeper.clustering.PathNodeCacheEvent.Type.NODE_UPDATED;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.utils.ThreadUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;
import com.ixaris.commons.collections.lib.BitSetIterator;
import com.ixaris.commons.misc.lib.conversion.HexUtil;
import com.ixaris.commons.misc.lib.id.Sequence;
import com.ixaris.commons.misc.lib.object.Reference;
import com.ixaris.commons.protobuf.lib.MessageHelper;
import com.ixaris.commons.zookeeper.clustering.CommonsZookeeperClustering.ClusterInfo;
import com.ixaris.commons.zookeeper.clustering.CommonsZookeeperClustering.NodeInfo;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClient;

/**
 * Zookeeper Service discovery Connection
 *
 * <p>This class is responsible to connect with Zookeeper and ensures we have only a single connection to zookeeper for
 * both Service Discovery and Registry. It also supports storing a set of listeners connected to zookeeper to ensure a
 * smooth/clean shutdown.
 *
 * @author aldrin.seychell
 */
public class ZookeeperClusterDiscoveryConnection {
    
    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperClusterDiscoveryConnection.class);
    
    private static final String PATH_SEQUENCES = "/discovery/sequences";
    private static final String PATH_CLUSTERS = "/discovery/clusters";
    
    static String getSequenceZnodePath(final String sequence) {
        return ZKPaths.makePath(PATH_SEQUENCES, sequence);
    }
    
    static String getSequenceGroupZnodePath(final String sequence, final String group) {
        return ZKPaths.makePath(getSequenceZnodePath(sequence), group);
    }
    
    static String getSequenceIdZnodePath(final String sequence, final String group, final String id) {
        return ZKPaths.makePath(getSequenceGroupZnodePath(sequence, group), id);
    }
    
    static String getClusterZnodePath(final String clusterName) {
        return ZKPaths.makePath(PATH_CLUSTERS, clusterName);
    }
    
    static String getClusterNodeZnodePath(final String clusterName, final String nodeId) {
        return ZKPaths.makePath(getClusterZnodePath(clusterName), nodeId);
    }
    
    static String nodeIdToName(final int nodeId) {
        return HexUtil.encode((byte) (nodeId >>> 8), (byte) nodeId);
    }
    
    static int nodeNameToId(final String nodeName) {
        byte[] decoded = HexUtil.decode(nodeName);
        return (((int) decoded[0] & 0xFF) << 8) | ((int) decoded[1] & 0xFF);
    }
    
    static byte[] encodeNodeInfo(final ClusterNodeInfo clusterNodeInfo) {
        final NodeInfo.Builder nodeInfo = NodeInfo.newBuilder()
            .setTimestamp(clusterNodeInfo.getTimestamp())
            .putAllAttribute(clusterNodeInfo.getAttributes());
        for (final BitSetIterator i = clusterNodeInfo.getShards().iterator(); i.hasNext();) {
            nodeInfo.addShard(i.next());
        }
        for (final BitSetIterator i = clusterNodeInfo.getShardsStopping().iterator(); i.hasNext();) {
            nodeInfo.addShardStopping(i.next());
        }
        return encode(MessageHelper.json(nodeInfo));
    }
    
    static byte[] encodeClusterInfo(final int maxShards) {
        final ClusterInfo.Builder nodeInfo = ClusterInfo.newBuilder().setMaxShards(maxShards);
        return encode(MessageHelper.json(nodeInfo));
    }
    
    public static byte[] encode(final String s) {
        return s.getBytes(UTF_8);
    }
    
    static <T extends MessageLite> T decode(final Class<T> type, final byte[] data) {
        final String decoded = decode(data);
        if ((decoded == null) || decoded.isEmpty()) {
            throw new IllegalStateException("Node data is empty");
        }
        try {
            return MessageHelper.parse(type, decoded);
        } catch (final InvalidProtocolBufferException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static String decode(final byte[] data) {
        if ((data == null) || (data.length == 0)) {
            return null;
        }
        return new String(data, UTF_8);
    }
    
    private final String clusterName;
    private final CuratorFramework zookeeperClient;
    private final ExecutorService cacheExecutorService;
    private final ZookeeperClusterSequences clusterSequences;
    private final Sequence nodeId;
    
    private final Object lock = new Object();
    private final Map<String, NodeCacheHolder> registeredNodeCaches = new HashMap<>();
    private final Map<String, PathChildrenCache> registeredChildCaches = new HashMap<>();
    private final ConnectionStateListener connectionStateListener = this::handleConnectionStateChanged;
    private final AtomicBoolean active = new AtomicBoolean(true);
    
    private long sessionId;
    private long prevSessionId;
    
    @SuppressWarnings("squid:S1699")
    public ZookeeperClusterDiscoveryConnection(final String clusterName, final ZookeeperClient zookeeperClient) {
        this.clusterName = clusterName;
        this.zookeeperClient = zookeeperClient.get();
        
        try {
            sessionId = this.zookeeperClient.getZookeeperClient().getZooKeeper().getSessionId();
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        prevSessionId = sessionId;
        
        final ThreadFactory pathCacheThreadFactory = ThreadUtils.newThreadFactory("NodeCache");
        cacheExecutorService = Executors.newSingleThreadExecutor(pathCacheThreadFactory);
        
        this.zookeeperClient.getConnectionStateListenable().addListener(connectionStateListener);
        
        clusterSequences = new ZookeeperClusterSequences(this);
        nodeId = clusterSequences.getSequence(clusterName);
        
        zookeeperClient.addCloseTask(this::shutdown);
    }
    
    public String getClusterName() {
        return clusterName;
    }
    
    public void shutdown() {
        if (active.compareAndSet(true, false)) {
            synchronized (lock) {
                LOG.debug("Initialising shutdown of service discovery client");
                
                clusterSequences.shutdown();
                
                zookeeperClient.getConnectionStateListenable().removeListener(connectionStateListener);
                
                // Close caches and zk client
                registeredNodeCaches.forEach((k, v) -> CloseableUtils.closeQuietly(v.cache));
                registeredChildCaches.forEach((k, v) -> CloseableUtils.closeQuietly(v));
                cacheExecutorService.shutdownNow();
            }
        }
    }
    
    boolean ownsEphemeralNode(final Stat stat) {
        return stat.getEphemeralOwner() == sessionId;
    }
    
    boolean ownedEphemeralNode(final Stat stat) {
        return stat.getEphemeralOwner() == prevSessionId;
    }
    
    @SuppressWarnings("squid:S1166")
    boolean createEphemeralNode(final String znodeId, final byte[] data) {
        try {
            if (data == null) {
                zookeeperClient.create().withMode(CreateMode.EPHEMERAL).forPath(znodeId);
            } else {
                zookeeperClient.create().withMode(CreateMode.EPHEMERAL).forPath(znodeId, data);
            }
            return true;
        } catch (final NodeExistsException e) {
            return false;
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    @SuppressWarnings({ "squid:S00108", "squid:S1166", "checkstyle:com.puppycrawl.tools.checkstyle.checks.blocks.EmptyCatchBlockCheck" })
    void removeEphemeralNode(final String znodeId) {
        try {
            zookeeperClient.delete().forPath(znodeId);
        } catch (final NoNodeException ignored) {} catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    public int getNodeId() {
        return nodeId.getNodeId();
    }
    
    protected CuratorFramework getZookeeperClient() {
        return zookeeperClient;
    }
    
    @SuppressWarnings("squid:S1147")
    void handleNodeRemoved() {
        if (active.get()) {
            LOG.error("Node was removed from service registry - will now shutdown", new IllegalStateException("Node removed"));
            new Thread("NodeRemovedShutdown") {
                
                @Override
                public void run() {
                    System.exit(201);
                }
                
            }.start();
        }
    }
    
    public void watchPath(final String path, final PathChildrenCacheListener listener) {
        if (active.get()) {
            synchronized (lock) {
                final Reference.Boolean shouldStartCache = new Reference.Boolean(false);
                final PathChildrenCache cache = registeredChildCaches.computeIfAbsent(path, k -> {
                    shouldStartCache.set(true);
                    return new PathChildrenCache(zookeeperClient, k, true, false, cacheExecutorService);
                });
                cache.getListenable().addListener(listener);
                
                if (shouldStartCache.get()) {
                    try {
                        cache.start();
                    } catch (final Exception e) {
                        throw new IllegalStateException(
                            String.format("Unable to start service node cache for %s", path), e);
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("squid:S134")
    public void unwatchPath(final String path, final PathChildrenCacheListener listener) {
        if (active.get()) {
            synchronized (lock) {
                final PathChildrenCache cache = registeredChildCaches.get(path);
                if (cache != null) {
                    cache.getListenable().removeListener(listener);
                    if (cache.getListenable().size() == 0) {
                        try {
                            cache.close();
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                        registeredChildCaches.remove(path);
                    }
                }
            }
        }
    }
    
    public void watchNode(final String path, final PathNodeCacheListener listener) {
        if (active.get()) {
            synchronized (lock) {
                final Reference.Boolean shouldStartCache = new Reference.Boolean(false);
                final NodeCacheHolder holder = registeredNodeCaches.computeIfAbsent(path, k -> {
                    shouldStartCache.set(true);
                    return new NodeCacheHolder(new NodeCache(zookeeperClient, k, false));
                });
                holder.cache
                    .getListenable()
                    .addListener(holder.conversion.computeIfAbsent(listener, k -> () -> {
                        final ChildData data = holder.cache.getCurrentData();
                        k.nodeEvent(zookeeperClient, data != null
                            ? new PathNodeCacheEvent(NODE_UPDATED, data)
                            : new PathNodeCacheEvent(NODE_REMOVED, new ChildData(path, null, null)));
                    }));
                
                if (shouldStartCache.get()) {
                    try {
                        holder.cache.start();
                    } catch (final Exception e) {
                        throw new IllegalStateException(
                            String.format("Unable to start service node cache for %s", path), e);
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("squid:S134")
    public void unwatchNode(final String path, final PathNodeCacheListener listener) {
        if (active.get()) {
            synchronized (lock) {
                final NodeCacheHolder holder = registeredNodeCaches.get(path);
                if (holder != null) {
                    holder.cache.getListenable().removeListener(holder.conversion.get(listener));
                    if (holder.cache.getListenable().size() == 0) {
                        try {
                            holder.cache.close();
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                        registeredNodeCaches.remove(path);
                    }
                }
            }
        }
    }
    
    private static final class NodeCacheHolder {
        
        private final NodeCache cache;
        private final Map<PathNodeCacheListener, NodeCacheListener> conversion = new WeakHashMap<>();
        
        private NodeCacheHolder(final NodeCache cache) {
            this.cache = cache;
        }
        
    }
    
    @SuppressWarnings("squid:S1172")
    private void handleConnectionStateChanged(final CuratorFramework client, final ConnectionState state) {
        try {
            if (state.isConnected()) {
                long newSessionId = zookeeperClient.getZookeeperClient().getZooKeeper().getSessionId();
                if (newSessionId != sessionId) {
                    prevSessionId = sessionId;
                    sessionId = newSessionId;
                }
            }
        } catch (final Exception e) {
            LOG.error("Error in handleConnectionStateChanged", e);
        }
    }
    
}
