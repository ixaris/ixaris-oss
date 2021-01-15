package com.ixaris.commons.zookeeper.clustering;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.clustering.lib.common.ClusterSequence;
import com.ixaris.commons.misc.lib.conversion.HexUtil;
import com.ixaris.commons.misc.lib.id.Sequence;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.misc.lib.id.UpdateableSequence;

/**
 * Resolve a cluster wide unique identifier from 0x8 to 0xFFFF
 */
public final class ZookeeperClusterSequences implements ClusterSequence {
    
    public static final int RESERVED_IN_FIRST_GROUP = 8;
    
    private static final int MAX_INDEX = 0xFF;
    private static final int INDEX_TO_CREATE_NEXT_GROUP = 0xCF;
    private static final int INDEX_TO_DELETE_NEXT_GROUP = 0x9F;
    
    public static int[] decode(final List<String> children) {
        final int[] decodedIds = new int[children.size()];
        int i = 0;
        for (final String child : children) {
            decodedIds[i] = HexUtil.decode(child)[0] & MAX_INDEX;
            i++;
        }
        Arrays.sort(decodedIds); // sort children
        return decodedIds;
    }
    
    /**
     * O(log(n)) given a 0-based list of ids, finds the first gap
     */
    public static int findFirstGap(final int[] ids) {
        int end = ids.length - 1;
        if (end == -1) {
            return 0;
        }
        if (ids[end] == end) {
            return end + 1;
        }
        return findFirstGap(ids, 0, end);
    }
    
    private static int findFirstGap(final int[] ids, final int start, final int end) {
        if (ids[start] != start) {
            return start;
        }
        int mid = (start + end) / 2;
        if (ids[mid] == mid) {
            int nextStart = mid + 1;
            if (nextStart == end) {
                return nextStart;
            } else {
                return findFirstGap(ids, nextStart, end); // gap is in last half
            }
        } else {
            return findFirstGap(ids, 0, mid); // gap is in first half
        }
    }
    
    private static int determineNodeWidthFromGroup(final int group) {
        if (group == 0) {
            return 8;
        } else if (group < 2) {
            return 9;
        } else if (group < 4) {
            return 10;
        } else if (group < 8) {
            return 11;
        } else if (group < 16) {
            return 12;
        } else if (group < 32) {
            return 13;
        } else if (group < 64) {
            return 14;
        } else if (group < 128) {
            return 15;
        } else {
            return 16;
        }
    }
    
    private static int makeNodeId(final int group, final int index) {
        return (group << 8) | index;
    }
    
    private static byte extractGroup(final int nodeId) {
        return (byte) (nodeId >>> 8);
    }
    
    private static byte extractIndexInGroup(final int nodeId) {
        return (byte) nodeId;
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperClusterSequences.class);
    private static final AtomicReference<ZookeeperClusterDiscoveryConnection> DEFAULT_CONNECTION = new AtomicReference<>();
    
    private final ZookeeperClusterDiscoveryConnection connection;
    private final Map<String, UpdateableSequence> sequences = new HashMap<>();
    
    private final PathNodeCacheListener nodeIdListener = this::handleNodeChangedOrRemoved;
    private final PathChildrenCacheListener nodeWidthListener = this::handleGroupChildrenUpdated;
    
    ZookeeperClusterSequences(final ZookeeperClusterDiscoveryConnection connection) {
        this.connection = connection;
        if (DEFAULT_CONNECTION.compareAndSet(null, connection)) {
            // only fetch the default sequence on the first connection from the node.
            // Eventually might want to hide this behind a system variable to disable for large clusters as this forces
            // the total nodes to be < (65535 - 8)
            getSequence(DEFAULT);
        }
    }
    
    @SuppressWarnings("squid:ForLoopCounterChangedCheck")
    private void resolveUniqueId(final String name, final UpdateableSequence sequence) {
        for (int lastGroup = 0; lastGroup <= MAX_INDEX;) {
            final String znodeGroup = ZookeeperClusterDiscoveryConnection.getSequenceGroupZnodePath(
                name, HexUtil.encode((byte) lastGroup));
            final List<String> children = ensureGroupCreatedAndGetChildrenOrNullIfFull(lastGroup, znodeGroup);
            if (children != null) {
                final int[] decoded = decode(children);
                final int nextIndexInGroup = findFirstGap(decoded);
                final String znodeIdPath = ZKPaths.makePath(znodeGroup, HexUtil.encode((byte) nextIndexInGroup));
                if (connection.createEphemeralNode(znodeIdPath, null)) {
                    connection.watchNode(znodeIdPath, nodeIdListener);
                    connection.watchPath(ZookeeperClusterDiscoveryConnection.getSequenceZnodePath(name), nodeWidthListener);
                    final int group = lastGroup;
                    final int lastChild = decoded.length > 0
                        ? Math.max(nextIndexInGroup, decoded[decoded.length - 1]) : nextIndexInGroup;
                    lastGroup = createOrDeleteNextGroupsIfRequired(name, lastChild, group);
                    final int newNodeWidth = determineNodeWidthFromGroup(lastGroup);
                    sequence.setNodeId(newNodeWidth, makeNodeId(group, nextIndexInGroup));
                    return;
                }
            } else {
                lastGroup++;
            }
        }
        throw new IllegalStateException("Unable to generate unique node ID in Zookeeper - All ids exhausted");
    }
    
    /**
     * we create next group if the id is > 0xC0 (192), to preempt widening before it is actually required thus, we are
     * 64 nodes ahead of needing to update the node width, since we only really need the next group when we get to 0xFF
     */
    @SuppressWarnings({ "squid:S1166", "squid:S2189" })
    private int createOrDeleteNextGroupsIfRequired(final String name, final int lastIdInGroup, final int lastGroup) {
        if (lastIdInGroup < INDEX_TO_DELETE_NEXT_GROUP) {
            int nextGroup = lastGroup + 1;
            while (true) {
                try {
                    connection
                        .getZookeeperClient()
                        .delete()
                        .forPath(ZookeeperClusterDiscoveryConnection.getSequenceGroupZnodePath(
                            name, HexUtil.encode((byte) nextGroup)));
                } catch (final NoNodeException e) {
                    return lastGroup; // stop when node does not exists
                } catch (final RuntimeException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        } else if ((lastIdInGroup >= INDEX_TO_CREATE_NEXT_GROUP) && (lastGroup < MAX_INDEX)) {
            try {
                connection
                    .getZookeeperClient()
                    .create()
                    .forPath(ZookeeperClusterDiscoveryConnection.getSequenceGroupZnodePath(
                        name, HexUtil.encode((byte) (lastGroup + 1))));
                return lastGroup + 1;
            } catch (final NodeExistsException e) {
                return lastGroup + 1; // we're fine if it already exists - created by another node
            } catch (final RuntimeException e) {
                throw e;
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            return lastGroup;
        }
    }
    
    /**
     * @return the list of children, or null if children for this group are already full (shortcut to avoid getting
     *     children unnecessarily)
     */
    @SuppressWarnings({ "squid:S00108", "squid:S1166", "checkstyle:com.puppycrawl.tools.checkstyle.checks.blocks.EmptyCatchBlockCheck" })
    private List<String> ensureGroupCreatedAndGetChildrenOrNullIfFull(final int lastGroup, final String znodeGroup) {
        final CuratorFramework zookeeperClient = connection.getZookeeperClient();
        try {
            int numChildren;
            final Stat groupStat = zookeeperClient.checkExists().forPath(znodeGroup);
            if (groupStat == null) {
                try {
                    zookeeperClient.create().creatingParentsIfNeeded().forPath(znodeGroup);
                } catch (final NodeExistsException ignored) {
                    // no problem
                }
                numChildren = 0;
            } else {
                numChildren = groupStat.getNumChildren();
            }
            
            if (lastGroup == 0) {
                numChildren += RESERVED_IN_FIRST_GROUP;
            }
            // Use <= because FF is 255 and there are 256 possible children (0 - FF inclusive)
            if (numChildren <= MAX_INDEX) {
                final List<String> children = zookeeperClient.getChildren().forPath(znodeGroup);
                if (lastGroup == 0) {
                    // add reserved children
                    for (int i = 0; i < RESERVED_IN_FIRST_GROUP; i++) {
                        children.add(HexUtil.encode((byte) i));
                    }
                }
                return (children.size() <= MAX_INDEX) ? children : null;
            } else {
                return null;
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    public void shutdown() {
        final CuratorFramework zookeeperClient = connection.getZookeeperClient();
        synchronized (sequences) {
            sequences.forEach((name, sequence) -> {
                connection.unwatchPath(ZookeeperClusterDiscoveryConnection.getSequenceZnodePath(name), nodeWidthListener);
                final int nodeId = sequence.getNodeId();
                final String idPath = ZookeeperClusterDiscoveryConnection.getSequenceIdZnodePath(
                    name, HexUtil.encode(extractGroup(nodeId)), HexUtil.encode(extractIndexInGroup(nodeId)));
                connection.unwatchNode(idPath, nodeIdListener);
                
                if (zookeeperClient.getZookeeperClient().isConnected()) {
                    try {
                        connection.removeEphemeralNode(idPath);
                    } catch (final RuntimeException e) {
                        LOG.warn("failed to remove ephemeral node for " + idPath, e);
                    }
                }
                // If we are not still connected to zookeeper, we can only rely on ephemeral nodes to be removed
                // automatically
            });
        }
        DEFAULT_CONNECTION.compareAndSet(connection, null);
    }
    
    @Override
    public Sequence getSequence(final String name) {
        synchronized (sequences) {
            return sequences.computeIfAbsent(name, k -> {
                final UpdateableSequence sequence = DEFAULT.equals(k)
                    ? UniqueIdGenerator.DEFAULT_SEQUENCE : new UpdateableSequence();
                resolveUniqueId(k, sequence);
                return sequence;
            });
        }
    }
    
    private void updateNodeWidthFromGroup(final UpdateableSequence sequence, final int group, final boolean added) {
        if (added) {
            final int newNodeWidth = determineNodeWidthFromGroup(group);
            if (newNodeWidth > sequence.getNodeWidth()) {
                sequence.setNodeId(newNodeWidth, sequence.getNodeId());
            }
        } else {
            final int newNodeWidth = determineNodeWidthFromGroup(group - 1);
            if (newNodeWidth < sequence.getNodeWidth()) {
                sequence.setNodeId(newNodeWidth, sequence.getNodeId());
            }
        }
    }
    
    private void handleGroupChildrenUpdated(final CuratorFramework client, final PathChildrenCacheEvent event) {
        if (event.getData() == null) {
            LOG.warn("Event with no data will be ignored: {}", event);
            return;
        }
        
        // path is something like {base_path}/sequences/{SEQUENCE_NAME}/{GROUP}
        final String[] pathParts = event.getData().getPath().split(ZKPaths.PATH_SEPARATOR, -1);
        final int group = HexUtil.decode(pathParts[pathParts.length - 1])[0] & MAX_INDEX;
        final String sequenceName = pathParts[pathParts.length - 2];
        if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED) {
            synchronized (sequences) {
                updateNodeWidthFromGroup(sequences.get(sequenceName), group, true);
            }
        } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
            synchronized (sequences) {
                updateNodeWidthFromGroup(sequences.get(sequenceName), group, false);
            }
        }
    }
    
    private void handleNodeChangedOrRemoved(final CuratorFramework client, final PathNodeCacheEvent event) {
        if (!reclaimNodeId(event.getData().getPath(), event.getData().getStat())) {
            // no longer owner of id
            connection.handleNodeRemoved();
        }
    }
    
    @SuppressWarnings("squid:S1166")
    private boolean reclaimNodeId(final String path, final Stat stat) {
        if (stat != null) {
            if (connection.ownsEphemeralNode(stat)) {
                return true; // still own id
            } else if (connection.ownedEphemeralNode(stat)) {
                try {
                    connection.removeEphemeralNode(path);
                    return (connection.createEphemeralNode(path, null)); // own id reclaimed
                } catch (final RuntimeException e) {
                    LOG.warn("failed to remove or recreate ephemeral node for " + path, e);
                }
            }
        } else {
            // node removed so try to reclaim own id
            try {
                return connection.createEphemeralNode(path, null); // own id reclaimed
            } catch (final RuntimeException e) {
                LOG.warn("failed to recreate ephemeral node for " + path);
            }
        }
        return false;
    }
    
}
