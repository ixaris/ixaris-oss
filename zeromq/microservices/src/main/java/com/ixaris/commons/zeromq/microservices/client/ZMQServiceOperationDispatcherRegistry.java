package com.ixaris.commons.zeromq.microservices.client;

import static com.ixaris.commons.async.lib.CompletableFutureUtil.reject;
import static com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport.extractTimeout;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQException;

import com.google.protobuf.InvalidProtocolBufferException;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.AsyncTrace;
import com.ixaris.commons.async.lib.FutureAsync;
import com.ixaris.commons.async.lib.scheduler.Scheduler;
import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;
import com.ixaris.commons.clustering.lib.common.ClusterShardResolver;
import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.common.ClusterTopologyChangeListener;
import com.ixaris.commons.collections.lib.IntMap;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceDiscovery;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceEndpoint;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceEndpointChangeListener;
import com.ixaris.commons.microservices.lib.client.support.ServiceOperationDispatcher;
import com.ixaris.commons.microservices.lib.common.PendingKey;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.zeromq.microservices.ZMQGlobal;
import com.ixaris.commons.zeromq.microservices.ZMQGlobal.ZMQAfterContextShutdownThread;
import com.ixaris.commons.zeromq.microservices.common.ZMQWakeThread;

public final class ZMQServiceOperationDispatcherRegistry extends ZMQAfterContextShutdownThread {
    
    private static final Logger LOG = LoggerFactory.getLogger(ZMQServiceOperationDispatcherRegistry.class);
    
    private static final class PendingOperation {
        
        private final ZMQServiceOperationDispatcher dispatcher;
        private final RequestEnvelope request;
        private final BiConsumer<? super ResponseEnvelope, ? super Throwable> relayConsumer;
        
        private int retry = 20;
        private ScheduledFuture<?> scheduledTask;
        
        private PendingOperation(final ZMQServiceOperationDispatcher dispatcher,
                                 final RequestEnvelope request,
                                 final FutureAsync<ResponseEnvelope> future) {
            this.dispatcher = dispatcher;
            this.request = request;
            relayConsumer = AsyncExecutor.relayConsumer(future);
        }
        
    }
    
    private final ScheduledExecutorService executor;
    private final ServiceDiscovery serviceDiscovery;
    private final ZMQProxySettings proxySettings;
    
    private final ZMQWakeThread wakeThread;
    private final Map<String, ZMQServiceOperationDispatcher> dispatchers = new HashMap<>();
    private final Map<String, ZMQCluster> clusters = new HashMap<>();
    private final ConcurrentLinkedQueue<Tuple3<String, CompletableFuture<ZMQServiceOperationDispatcher>, AsyncTrace>> dispatcherRegistrations = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PendingOperation> pendingOperationsQueue = new ConcurrentLinkedQueue<>();
    private final Map<PendingKey, PendingOperation> waitingOperationResponses = new ConcurrentHashMap<>();
    
    public ZMQServiceOperationDispatcherRegistry(final ScheduledExecutorService executor,
                                                 final ServiceDiscovery serviceDiscovery) {
        this(executor, serviceDiscovery, null);
    }
    
    public ZMQServiceOperationDispatcherRegistry(final ScheduledExecutorService executor,
                                                 final ServiceDiscovery serviceDiscovery,
                                                 final ZMQProxySettings proxySettings) {
        super(ZMQServiceOperationDispatcherRegistry.class.getSimpleName());
        
        this.executor = executor;
        this.serviceDiscovery = serviceDiscovery;
        this.proxySettings = proxySettings;
        this.wakeThread = new ZMQWakeThread(getName() + "-WAKE");
        
        start();
    }
    
    public ZMQServiceOperationDispatcher register(final String name) {
        final CompletableFuture<ZMQServiceOperationDispatcher> future = new CompletableFuture<>();
        final AsyncTrace trace = AsyncTrace.get();
        dispatcherRegistrations.offer(tuple(name, future, trace));
        wakeThread.offerWake();
        
        try {
            return future.get(ZMQGlobal.REGISTRATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (final InterruptedException | TimeoutException | ExecutionException e) {
            throw new IllegalStateException("Error while waiting for registration confirmation", e);
        }
    }
    
    @SuppressWarnings("squid:S1141")
    @Override
    public void run() {
        // context is not within try-with-resources since it is managed by ZMQ Global with shutdown functionality
        final Context context = ZMQGlobal.getContext();
        
        try (ZMQ.Socket pull = context.socket(ZMQ.PULL)) {
            
            final ZMQ.Poller poller = context.poller(16);
            
            pull.bind("inproc://" + wakeThread.getName());
            poller.register(pull, ZMQ.Poller.POLLIN);
            
            wakeThread.start();
            
            while (!interrupted()) {
                // if poll interrupted returns value less than 0
                if (poller.poll() < 0) {
                    break;
                }
                
                try {
                    updateTopology(poller);
                    sendPendingRequestsAndScheduleTimeout();
                    processPendingResponsesAndCancelTimeout(poller);
                    handlePendingRegistrations();
                } catch (final RuntimeException e) {
                    LOG.error("Error in " + getClass().getName(), e);
                }
                
                if (poller.pollin(0)) {
                    pull.recv(); // consume all wait data
                }
            }
            
        } catch (final ZMQException e) {
            if (e.getErrorCode() != ZMQ.Error.ETERM.getCode()) {
                LOG.error("error in " + getClass().getName(), e);
                throw e;
            }
            
        } catch (final Exception e) {
            LOG.error("error in " + getClass().getName(), e);
            throw e;
            
        } finally {
            for (final ZMQServiceOperationDispatcher dispatcher : dispatchers.values()) {
                dispatcher.close();
            }
            dispatchers.clear();
            for (final ZMQCluster cluster : clusters.values()) {
                cluster.close();
            }
            clusters.clear();
        }
    }
    
    private void updateTopology(final Poller poller) {
        for (final ZMQServiceOperationDispatcher dispatcher : dispatchers.values()) {
            dispatcher.updateConnections(poller);
        }
        for (final ZMQCluster cluster : clusters.values()) {
            cluster.updateConnections();
        }
    }
    
    private void sendPendingRequestsAndScheduleTimeout() {
        PendingOperation pendingOperation;
        while ((pendingOperation = pendingOperationsQueue.poll()) != null) {
            final RequestEnvelope requestEnvelope = pendingOperation.request;
            final ZMQServiceOperationDispatcher dispatcher = pendingOperation.dispatcher;
            
            final String clusterName = dispatcher.keyToClusterName.get(requestEnvelope.getServiceKey());
            final ZMQConnection dealer = Optional.ofNullable(clusters.get(clusterName)).map(c -> c.dealer).orElse(null);
            if ((dealer == null) || dealer.urls.isEmpty()) {
                // when custer is not known or has no nodes, give some grace time for reconnections
                // wait maximum of 2 seconds in 100 ms intervals
                
                if (--pendingOperation.retry >= 0) {
                    final PendingOperation retryOperation = pendingOperation;
                    Scheduler
                        .commonScheduler()
                        .schedule(
                            () -> {
                                pendingOperationsQueue.offer(retryOperation);
                                wakeThread.offerWake();
                            },
                            100L,
                            TimeUnit.MILLISECONDS);
                } else {
                    LOG.error("Publishing unavailable reply [{}:{}] for [{}]",
                        requestEnvelope.getCorrelationId(),
                        requestEnvelope.getCallRef(),
                        dispatcher.name);
                    
                    pendingOperation.relayConsumer.accept(ResponseEnvelope.newBuilder()
                        .setCorrelationId(requestEnvelope.getCorrelationId())
                        .setCallRef(requestEnvelope.getCallRef())
                        .setStatusCode(ResponseStatusCode.SERVER_UNAVAILABLE)
                        .build(), null);
                }
            } else {
                final PendingKey key = PendingKey.from(requestEnvelope);
                final PendingOperation duplicate = waitingOperationResponses.put(key, pendingOperation);
                if (duplicate != null) {
                    cancelOperationDueToTimeout(duplicate, "Duplicate concurrent request");
                }
                
                pendingOperation.scheduledTask = executor.schedule(
                    () -> cancelOperationDueToTimeout(waitingOperationResponses.remove(key), "Timed out"),
                    extractTimeout(pendingOperation.request),
                    TimeUnit.MILLISECONDS);
                
                try {
                    final Boolean sendResult = doUninterruptableWork(true, () -> dealer.socket.send(requestEnvelope.toByteArray()));
                    if (sendResult == null || !sendResult) {
                        LOG.warn(
                            "Failed to send request [{}:{}] for [{}/{}] on urls [{}]",
                            requestEnvelope.getCorrelationId(),
                            requestEnvelope.getCallRef(),
                            dispatcher.name,
                            requestEnvelope.getServiceKey(),
                            dealer.urls);
                    }
                } catch (final RuntimeException t) {
                    LOG.error("ERROR while sending requests", t);
                }
            }
        }
    }
    
    private void cancelOperationDueToTimeout(final PendingOperation pendingOperation, final String message) {
        // It is possible that the callback was consumed whilst servicing a response. In this case ignore timeout.
        if (pendingOperation != null) {
            final RequestEnvelope requestEnvelope = pendingOperation.request;
            final ZMQServiceOperationDispatcher dispatcher = pendingOperation.dispatcher;
            
            LOG.debug("Publishing timeout response [{}:{}] for [{}]",
                requestEnvelope.getCorrelationId(),
                requestEnvelope.getCallRef(),
                dispatcher.name);
            pendingOperation.relayConsumer.accept(ResponseEnvelope.newBuilder()
                .setCorrelationId(requestEnvelope.getCorrelationId())
                .setCallRef(requestEnvelope.getCallRef())
                .setStatusCode(ResponseStatusCode.SERVER_TIMEOUT)
                .setStatusMessage(message)
                .build(), null);
        }
    }
    
    @SuppressWarnings("squid:S134")
    private void processPendingResponsesAndCancelTimeout(final Poller poller) {
        for (final ZMQCluster cluster : clusters.values()) {
            if (poller.pollin(cluster.dealer.index)) {
                final byte[] payload = doUninterruptableWork(true, cluster.dealer.socket::recv);
                if (payload != null) {
                    try {
                        final ResponseEnvelope responseEnvelope = ResponseEnvelope.parseFrom(payload);
                        final PendingKey key = PendingKey.from(responseEnvelope);
                        completeOperation(waitingOperationResponses.remove(key), responseEnvelope);
                    } catch (final InvalidProtocolBufferException e) {
                        // we consume this by logging, ignoring and letting it timeout
                        LOG.error("error while parsing ResponseEnvelope", e);
                    }
                }
            }
        }
    }
    
    private void completeOperation(final PendingOperation pendingOperation, final ResponseEnvelope responseEnvelope) {
        // It is possible that the callback was consumed by a timeout before the response was obtained.
        // In this case ignore response.
        if (pendingOperation != null) {
            pendingOperation.scheduledTask.cancel(false); // try to cancel the timeout
            pendingOperation.relayConsumer.accept(responseEnvelope, null);
        }
    }
    
    private void handlePendingRegistrations() {
        Tuple3<String, CompletableFuture<ZMQServiceOperationDispatcher>, AsyncTrace> registration;
        while ((registration = dispatcherRegistrations.poll()) != null) {
            final String serviceName = registration.get1();
            final CompletableFuture<ZMQServiceOperationDispatcher> future = registration.get2();
            AsyncTrace.exec(registration.get3(), () -> {
                if (dispatchers.containsKey(serviceName)) {
                    reject(
                        future,
                        new IllegalStateException("Already registered operation dispatcher for " + serviceName));
                }
                final ZMQServiceOperationDispatcher dispatcher = new ZMQServiceOperationDispatcher(serviceName);
                dispatchers.put(serviceName, dispatcher);
                // Confirm registration
                future.complete(dispatcher);
            });
        }
    }
    
    @SuppressWarnings("squid:S2972")
    private final class ZMQServiceOperationDispatcher implements ServiceOperationDispatcher, ServiceEndpointChangeListener {
        
        private final String name;
        
        private final AtomicReference<ServiceEndpoint> endpoint = new AtomicReference<>();
        private final Map<String, String> keyToClusterName = new HashMap<>();
        
        private ZMQServiceOperationDispatcher(final String name) {
            this.name = name;
            serviceDiscovery.addEndpointListener(name, this);
        }
        
        @Override
        public Async<ResponseEnvelope> dispatch(final RequestEnvelope requestEnvelope) {
            final FutureAsync<ResponseEnvelope> future = new FutureAsync<>();
            final PendingOperation pendingOperation = new PendingOperation(this, requestEnvelope, future);
            pendingOperationsQueue.offer(pendingOperation);
            wakeThread.offerWake();
            return future;
        }
        
        @Override
        public boolean isKeyAvailable(final String key) {
            return keyToClusterName.containsKey(key);
        }
        
        @Override
        public void onEndpointChanged(final ServiceEndpoint endpoint) {
            this.endpoint.set(endpoint);
            wakeThread.offerWake();
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName() + " for " + name;
        }
        
        private void close() {
            serviceDiscovery.removeEndpointListener(name, this);
            for (final String clusterName : keyToClusterName.values()) {
                clusters.get(clusterName).remove();
            }
        }
        
        /**
         * Will never be called concurrently, managed by registry thread
         *
         * @param poller
         */
        @SuppressWarnings("squid:S134")
        private void updateConnections(final Poller poller) {
            final ServiceEndpoint t = endpoint.getAndSet(null);
            if (t != null) {
                LOG.debug("Received new Topology Update {} for {}", t, name);
                final Map<String, String> oldKeyToClusterName = new HashMap<>(keyToClusterName);
                for (final Entry<String, String> entry : t.getServiceKeys().entrySet()) {
                    final String key = entry.getKey();
                    final String clusterName = (proxySettings != null && proxySettings.isProxied(entry.getValue()))
                        ? proxySettings.getProxyCluster() : entry.getValue();
                    final String old = keyToClusterName.put(key, clusterName);
                    oldKeyToClusterName.remove(key);
                    if (old != null) {
                        if (!old.equals(clusterName)) {
                            clusters.get(old).remove();
                            clusters.computeIfAbsent(clusterName, n -> new ZMQCluster(n, poller)).add();
                        }
                    } else {
                        clusters.computeIfAbsent(clusterName, n -> new ZMQCluster(n, poller)).add();
                    }
                }
                for (final Entry<String, String> entry : oldKeyToClusterName.entrySet()) {
                    keyToClusterName.remove(entry.getKey());
                    clusters.get(entry.getValue()).remove();
                }
            }
        }
        
    }
    
    private static final class ZMQConnection {
        
        final Set<String> urls = new HashSet<>();
        final ZMQ.Socket socket;
        final int index;
        
        ZMQConnection(final ZMQ.Socket socket, final int index) {
            this.socket = socket;
            this.index = index;
        }
        
    }
    
    @SuppressWarnings("squid:S2972")
    private final class ZMQCluster implements ClusterTopologyChangeListener {
        
        private final String name;
        private final AtomicReference<ClusterTopology> topology = new AtomicReference<>();
        private final ZMQConnection dealer;
        private int count = 0;
        
        private ZMQCluster(final String name, final Poller poller) {
            this.name = name;
            final ZMQ.Socket socket = ZMQGlobal.getContext().socket(ZMQ.DEALER);
            final int index = poller.register(socket, Poller.POLLIN);
            dealer = new ZMQConnection(socket, index);
        }
        
        @Override
        public void onTopologyChanged(final ClusterTopology topology) {
            this.topology.set(topology);
            wakeThread.offerWake();
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName() + " for " + name;
        }
        
        private void add() {
            if (count == 0) {
                serviceDiscovery.addTopologyListener(name, this);
            }
            count++;
        }
        
        private void remove() {
            count--;
            if (count == 0) {
                serviceDiscovery.removeTopologyListener(name, this);
                topology.set(new ClusterTopology(ClusterShardResolver.DEFAULT));
                updateConnections();
            }
        }
        
        private void close() {
            try {
                dealer.socket.close();
            } catch (final RuntimeException e) {
                LOG.warn("error closing dispatcher dealer ZMQ socket", e);
            }
        }
        
        /**
         * Will never be called concurrently, managed by registry thread
         */
        @SuppressWarnings("squid:S134")
        private void updateConnections() {
            final ClusterTopology t = topology.getAndSet(null);
            if (t != null) {
                LOG.debug("Received new Topology Update {} for {}", t, name);
                final Set<String> deadUrls = new HashSet<>(dealer.urls);
                for (final Map.Entry<Integer, ClusterNodeInfo> node : t.getNodes().entrySet()) {
                    final String url = node.getValue().getAttributes().get(ZMQGlobal.OPERATION_URL_KEY);
                    if (url != null) {
                        deadUrls.remove(url);
                        if (!dealer.urls.contains(url)) {
                            try {
                                dealer.socket.setImmediate(false);
                                dealer.socket.setTCPKeepAlive(1);
                                dealer.socket.connect(url);
                                dealer.urls.add(url);
                                LOG.info("Connected with {} for {}", url, name);
                            } catch (final RuntimeException e) {
                                dealer.urls.remove(url);
                                LOG.error("Connection with {} for {} failed", url, name, e);
                            }
                        }
                    }
                }
                for (final String url : deadUrls) {
                    try {
                        dealer.socket.disconnect(url);
                        dealer.urls.remove(url);
                        LOG.info("Disconnected with {} for {}", url, name);
                    } catch (final RuntimeException e) {
                        LOG.error("Disconnection with {} for {} failed", url, name, e);
                    }
                }
            }
        }
        
    }
    
}
