package com.ixaris.commons.netty.clustering;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.clustering.lib.common.ClusterShardResolver.getShardKeyFromString;
import static com.ixaris.commons.clustering.lib.service.AbstractClusterRegistry.extractTimeout;
import static com.ixaris.commons.clustering.lib.service.ClusterRegistry.SHARD;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.AsyncQueue;
import com.ixaris.commons.async.lib.FutureAsync;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterRequestEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterResponseEnvelope;
import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;
import com.ixaris.commons.clustering.lib.common.ClusterShardResolver;
import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ClusterRouteHandler;
import com.ixaris.commons.clustering.lib.service.ClusterRouteTimeoutException;
import com.ixaris.commons.clustering.lib.service.ShardNotLocalException;
import com.ixaris.commons.collections.lib.BitSet;
import com.ixaris.commons.collections.lib.BitSetIterator;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.netty.clustering.CommonsNettyClustering.NettyRequestEnvelope;
import com.ixaris.commons.netty.clustering.CommonsNettyClustering.NettyResponseEnvelope;
import com.ixaris.commons.netty.clustering.CommonsNettyClustering.NettyShardStopped;
import com.ixaris.commons.netty.clustering.NettyBean.HostAndPort;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterRegistryHelper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

@SuppressWarnings("squid:S1200")
public final class NettyClusterSharding extends ZookeeperClusterRegistryHelper {
    
    private static final String CLUSTER_HOST_KEY = "NETTY_CLUSTER_HOST";
    private static final String CLUSTER_PORT_KEY = "NETTY_CLUSTER_PORT";
    private static final Logger LOG = LoggerFactory.getLogger(NettyClusterSharding.class);
    
    private static final class PendingRoute {
        
        private final BiConsumer<? super ClusterResponseEnvelope, ? super Throwable> relayConsumer;
        private ScheduledFuture<?> scheduledTask;
        
        private PendingRoute(final FutureAsync<ClusterResponseEnvelope> future) {
            relayConsumer = AsyncExecutor.relayConsumer(future);
        }
        
    }
    
    private static final class ShardInfo {
        
        // -ve means shard is not local - 0 is not used (1 / -1 means no in flight)
        // TODO rename to countInFlight
        private final AtomicInteger count = new AtomicInteger(-1);
        private final Map<Long, NettyRequestEnvelope> queue = new HashMap<>();
        
        private ShardInfo() {}
        
    }
    
    private final NettyBean nettyBean;
    private final ScheduledExecutorService executor;
    private final ShardStoppedHandler shardStoppedHandler;
    private final AsyncFilterNext<ClusterRequestEnvelope, ClusterResponseEnvelope> handleRouteChain;
    private final AsyncFilterNext<ClusterBroadcastEnvelope, Boolean> handleBroadcastChain;
    
    private final NettyServerChannel channel;
    private final ShardInfo[] shards;
    private final BitSet shardsStopping;
    private volatile ScheduledFuture<?> notifyShardsStoppedSchedule = null;
    
    private final AsyncQueue clusterQueue = new AsyncQueue();
    private volatile NettyCluster cluster = new NettyCluster(ClusterShardResolver.DEFAULT);
    private final Map<Long, PendingRoute> waitingRouteResponses = new HashMap<>();
    
    public NettyClusterSharding(final NettyBean nettyBean,
                                final ScheduledExecutorService executor,
                                final ClusterRegistry clusterRegistry,
                                final int nodeId,
                                final ShardStoppedHandler shardStoppedHandler,
                                final AsyncFilterNext<ClusterRequestEnvelope, ClusterResponseEnvelope> handleRouteChain,
                                final AsyncFilterNext<ClusterBroadcastEnvelope, Boolean> handleBroadcastChain) {
        super(nodeId);
        this.nettyBean = nettyBean;
        this.executor = executor;
        this.shardStoppedHandler = shardStoppedHandler;
        this.handleRouteChain = handleRouteChain;
        this.handleBroadcastChain = handleBroadcastChain;
        
        shards = new ShardInfo[clusterRegistry.getMaxShards()];
        for (int i = 0; i < shards.length; i++) {
            shards[i] = new ShardInfo();
        }
        shardsStopping = BitSet.of(clusterRegistry.getMaxShards());
        
        channel = new NettyServerChannel(nettyBean, NettyRequestEnvelope.getDefaultInstance(), this::handleRequest);
        
        clusterRegistry.mergeAttributes(ImmutableMap.of(CLUSTER_HOST_KEY, channel.getUrl().host, CLUSTER_PORT_KEY, Integer.toString(channel.getUrl().port)));
    }
    
    @SuppressWarnings("squid:S134")
    @Override
    public void updateTopology(final ClusterTopology topology) {
        clusterQueue
            .exec(() -> {
                final ClusterNodeInfo thisNode = updateCluster(topology);
                if (thisNode != null) {
                    for (int shard = 0; shard < shards.length; shard++) {
                        final ShardInfo shardInfo = shards[shard];
                        if (thisNode.getShardsStopping().contains(shard)) {
                            final int count = shardInfo.count.updateAndGet(c -> (c < 0) ? c : -c);
                            if (count == -1) {
                                stopShard(shard);
                            }
                        } else {
                            synchronized (shardsStopping) {
                                if (shardsStopping.remove(shard) && shardsStopping.isEmpty() && (notifyShardsStoppedSchedule != null)) {
                                    // cancel schedule
                                    notifyShardsStoppedSchedule.cancel(false);
                                    notifyShardsStoppedSchedule = null;
                                }
                            }
                            if (thisNode.getShards().contains(shard)) {
                                shardInfo.count.updateAndGet(c -> (c > 0) ? c : -c);
                                flushQueue(shard, shardInfo.queue, null);
                            } else {
                                final Channel shardChannel = cluster.getChannelForShard(shard);
                                if (shardChannel != null) {
                                    flushQueue(shard, shardInfo.queue, shardChannel);
                                }
                            }
                        }
                    }
                }
            })
            .onException(t -> LOG.error("Error while updating topology", t));
    }
    
    @SuppressWarnings("squid:S134")
    private ClusterNodeInfo updateCluster(final ClusterTopology topology) {
        final ImmutableMap.Builder<HostAndPort, NettyClientChannel> newNodes = ImmutableMap.builder();
        final Map<Integer, NettyClientChannel> newShards = new HashMap<>();
        final Map<HostAndPort, NettyClientChannel> nodes = new HashMap<>(cluster.getNodes());
        NettyClientChannel leader = null;
        ClusterNodeInfo thisNode = null;
        for (final Entry<Integer, ClusterNodeInfo> entry : topology.getNodes().entrySet()) {
            final int id = entry.getKey();
            final ClusterNodeInfo node = entry.getValue();
            if (id != nodeId) {
                final String host = node.getAttributes().get(CLUSTER_HOST_KEY);
                final String port = node.getAttributes().get(CLUSTER_PORT_KEY);
                if ((host != null) && (port != null)) {
                    final HostAndPort url = new HostAndPort(host, Integer.parseInt(port));
                    final NettyClientChannel nodeChannel = Optional.ofNullable(nodes.remove(url)).orElseGet(() -> {
                        LOG.info("Netty cluster connected with {}", url);
                        return new NettyClientChannel(executor, nettyBean, url, NettyResponseEnvelope.getDefaultInstance(), this::handleResponse);
                    });
                    newNodes.put(url, nodeChannel);
                    for (final BitSetIterator i = node.getShards().iterator(); i.hasNext();) {
                        final int shard = i.next();
                        newShards.put(shard, nodeChannel);
                        final int count = shards[shard].count.getAndUpdate(c -> (c < 0) ? c : -c);
                        if (count < -1) {
                            LOG.warn("Shard {} assigned to node {} while still stopping on this node {} ({} in flight)",
                                shard,
                                id,
                                nodeId,
                                count + 1);
                        } else if (count > 0) {
                            LOG.warn("Shard {} assigned to node {} while still active on this node {} ({} in flight)",
                                shard,
                                id,
                                nodeId,
                                count - 1);
                        }
                    }
                    if (id == topology.getLeaderNodeId()) {
                        leader = nodeChannel;
                    }
                }
            } else {
                thisNode = node;
            }
        }
        for (final Entry<HostAndPort, NettyClientChannel> node : nodes.entrySet()) {
            node.getValue().close();
        }
        cluster = new NettyCluster(topology.getLeaderNodeId(), newNodes.build(), newShards, leader, topology.getShardResolver());
        return thisNode;
    }
    
    @SuppressWarnings({ "squid:S134", "squid:S2445" })
    private void flushQueue(final int shard, final Map<Long, NettyRequestEnvelope> queue, final Channel channel) {
        synchronized (queue) {
            if (!queue.isEmpty()) {
                for (final Iterator<Entry<Long, NettyRequestEnvelope>> i = queue.entrySet().iterator(); i.hasNext();) {
                    final NettyRequestEnvelope requestEnvelope = i.next().getValue();
                    i.remove();
                    
                    if (channel == null) {
                        final PendingRoute pendingRoute;
                        synchronized (waitingRouteResponses) {
                            pendingRoute = waitingRouteResponses.remove(requestEnvelope.getRef());
                        }
                        
                        if (pendingRoute != null) {
                            pendingRoute.scheduledTask.cancel(false);
                            try {
                                forShard(shard, () -> handleRouteChain.next(requestEnvelope.getRoute())).map(r -> {
                                    pendingRoute.relayConsumer.accept(r, null);
                                    return null;
                                });
                            } catch (final ShardNotLocalException e) {
                                LOG.warn("Expecting shard {} to be local while flushing queue", shards, e);
                            }
                        }
                    } else {
                        channel
                            .writeAndFlush(requestEnvelope)
                            .addListener(writeFuture -> {
                                if (!writeFuture.isSuccess()) {
                                    LOG.error("Failed to send route [{}]", requestEnvelope.getRef(), writeFuture.cause());
                                }
                            });
                    }
                }
            }
        }
    }
    
    private void handleRequest(final ChannelHandlerContext ctx, final NettyRequestEnvelope requestEnvelope) {
        final long ref = requestEnvelope.getRef();
        switch (requestEnvelope.getMessageCase()) {
            case ROUTE:
                route(requestEnvelope.getRoute()).map(r -> ctx
                    .writeAndFlush(NettyResponseEnvelope.newBuilder().setRef(ref).setRoute(r).build())
                    .addListener(writeFuture -> {
                        if (!writeFuture.isSuccess()) {
                            LOG.error("Failed to send route ack [{}]", ref, writeFuture.cause());
                        }
                    }));
                break;
            case BROADCAST:
                executor.execute(() -> handleBroadcastChain.next(requestEnvelope.getBroadcast()));
                break;
            case SHARD_STOPPED:
                executor.execute(() -> shardStoppedHandler.handleShardStopped(requestEnvelope.getShardStopped().getNodeId(), requestEnvelope.getShardStopped().getShard()));
                break;
            default:
        }
    }
    
    @SuppressWarnings("squid:S1172")
    private void handleResponse(final ChannelHandlerContext ctx, final NettyResponseEnvelope responseEnvelope) {
        final long ref = responseEnvelope.getRef();
        switch (responseEnvelope.getMessageCase()) {
            case ROUTE:
                final PendingRoute pendingRoute;
                synchronized (waitingRouteResponses) {
                    pendingRoute = waitingRouteResponses.remove(ref);
                }
                
                // It is possible that the callback was consumed by a timeout before the response
                // was obtained, in which case, ignore response.
                if (pendingRoute != null) {
                    pendingRoute.scheduledTask.cancel(false);
                    pendingRoute.relayConsumer.accept(responseEnvelope.getRoute(), null);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown message case " + responseEnvelope.getMessageCase());
        }
    }
    
    @Override
    public <T, E extends Exception> Async<T> forShard(final int shard, final CallableThrows<Async<T>, E> callable) throws E, ShardNotLocalException {
        final ShardInfo shardInfo = shards[shard];
        // if positive, shard is local, so increment count of outstanding requests
        if (shardInfo.count.updateAndGet(c -> (c > 0) ? (c + 1) : c) > 0) {
            try {
                return awaitExceptions(SHARD.exec(shard, callable));
            } finally {
                // revert count increment
                final int count = shardInfo.count.getAndUpdate(c -> (c > 0) ? (c - 1) : (c + 1));
                if (count == -1) {
                    stopShard(shard);
                }
            }
        } else {
            throw new ShardNotLocalException();
        }
    }
    
    private void stopShard(final int shard) {
        synchronized (shardsStopping) {
            if (shardsStopping.isEmpty() && (notifyShardsStoppedSchedule == null)) {
                // schedule
                notifyShardsStoppedSchedule = executor.scheduleWithFixedDelay(this::notifyShardsStopped, 2, 2, TimeUnit.SECONDS);
            }
            shardsStopping.add(shard);
        }
        notifyShardStopped(shard);
    }
    
    private void notifyShardsStopped() {
        synchronized (shardsStopping) {
            for (final BitSetIterator i = shardsStopping.iterator(); i.hasNext();) {
                notifyShardStopped(i.next());
            }
        }
    }
    
    private void notifyShardStopped(final int shard) {
        if (nodeId == cluster.getLeaderNodeId()) {
            shardStoppedHandler.handleShardStopped(nodeId, shard);
        } else {
            final Channel leaderChannel = cluster.getLeaderChannel();
            if (leaderChannel != null) {
                leaderChannel.writeAndFlush(NettyRequestEnvelope.newBuilder()
                    .setRef(UniqueIdGenerator.generate())
                    .setShardStopped(NettyShardStopped.newBuilder().setNodeId(nodeId).setShard(shard).build()));
            }
        }
    }
    
    @Override
    public <RES extends MessageLite, REQ extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                               final long id,
                                                                               final String key,
                                                                               final REQ request,
                                                                               final DefaultRoute defaultRoute) throws ClusterRouteTimeoutException {
        final int shard = cluster.getShardResolver().getShard(key.isEmpty() ? id : getShardKeyFromString(key));
        try {
            return forShard(shard, () -> handler.handle(id, key, request));
        } catch (final ShardNotLocalException e) {
            return defaultRoute.defaultRoute(handler, id, key, request, r -> route(shard, r));
        }
    }
    
    @SuppressWarnings("squid:S1166")
    private Async<ClusterResponseEnvelope> route(final ClusterRequestEnvelope request) {
        final int shard = cluster.getShardResolver().getShard(request.getKey().isEmpty() ? request.getId() : getShardKeyFromString(request.getKey()));
        try {
            return forShard(shard, () -> handleRouteChain.next(request));
        } catch (final ShardNotLocalException e) {
            return route(request);
        }
    }
    
    private Async<ClusterResponseEnvelope> route(final int shard, final ClusterRequestEnvelope request) {
        final ShardInfo shardInfo = shards[shard];
        final long ref = UniqueIdGenerator.generate();
        final FutureAsync<ClusterResponseEnvelope> future = new FutureAsync<>();
        final PendingRoute pendingRoute = new PendingRoute(future);
        synchronized (waitingRouteResponses) {
            waitingRouteResponses.put(ref, pendingRoute);
        }
        
        pendingRoute.scheduledTask = executor.schedule(
            () -> {
                final PendingRoute waitingOperation;
                synchronized (waitingRouteResponses) {
                    waitingOperation = waitingRouteResponses.remove(ref);
                }
                
                // It is possible that the callback was consumed whilst servicing a response. In this case
                // ignore timeout.
                if (waitingOperation != null) {
                    LOG.debug("Sending timeout route ack [{}]", ref);
                    pendingRoute.relayConsumer.accept(
                        ClusterResponseEnvelope.newBuilder().setTimeout(true).build(), null);
                    synchronized (shardInfo.queue) {
                        shardInfo.queue.remove(ref);
                    }
                }
            },
            extractTimeout(request),
            TimeUnit.MILLISECONDS);
        
        final NettyRequestEnvelope requestEnvelope = NettyRequestEnvelope.newBuilder()
            .setRef(ref)
            .setRoute(request)
            .build();
        final Channel shardChannel = cluster.getChannelForShard(shard);
        if (shardChannel != null) {
            shardChannel
                .writeAndFlush(requestEnvelope)
                .addListener(writeFuture -> {
                    if (!writeFuture.isSuccess()) {
                        LOG.error("Failed to send route [{}]", ref, writeFuture.cause());
                    }
                });
        } else {
            synchronized (shardInfo.queue) {
                shardInfo.queue.put(ref, requestEnvelope);
            }
            
            if (shardInfo.count.get() > 0) {
                // double check maybe shard was assigned to this node in the meantime
                flushQueue(shard, shardInfo.queue, null);
            }
        }
        return future;
    }
    
    @Override
    public Async<Boolean> broadcast(final ClusterBroadcastEnvelope message) {
        // best effort broadcast
        cluster
            .getNodes()
            .values()
            .forEach(nc -> nc.getChannel().writeAndFlush(NettyRequestEnvelope.newBuilder()
                .setRef(UniqueIdGenerator.generate())
                .setBroadcast(message)
                .build()));
        return result(true);
    }
    
    @Override
    public void shutdown() {
        cluster.getNodes().values().forEach(NettyClientChannel::close);
        channel.close();
    }
    
}
