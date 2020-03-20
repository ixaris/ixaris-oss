package com.ixaris.commons.clustering.lib.service;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.misc.lib.exception.ExceptionUtil.sneakyThrow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.filter.AsyncFilterChain;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterRequestEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterResponseEnvelope;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.lock.LockUtil;
import com.ixaris.commons.misc.lib.object.Ordered;
import com.ixaris.commons.protobuf.lib.MessageHelper;

public abstract class AbstractClusterRegistry implements ClusterRegistry {
    
    public static <REQ extends MessageLite, RES extends MessageLite> Async<ClusterResponseEnvelope> handle(final ClusterRouteHandler<REQ, RES> handler,
                                                                                                           final ClusterRequestEnvelope request) {
        if (handler == null) {
            throw new IllegalStateException("Unmatched cluster route handler for " + request.getType());
        } else {
            final REQ parsed;
            try {
                parsed = MessageHelper.parse(ClusterRouteHandler.getRequestType(handler), request.getPayload());
            } catch (final InvalidProtocolBufferException e) {
                throw new IllegalStateException(e);
            }
            return handler
                .handle(request.getId(), request.getKey(), parsed)
                .map(response -> ClusterResponseEnvelope.newBuilder().setPayload(response.toByteString()).build());
        }
    }
    
    public static <T extends MessageLite> Async<Boolean> handle(final ClusterBroadcastHandler<T> handler, final ClusterBroadcastEnvelope request) {
        if (handler == null) {
            throw new IllegalStateException("Unmatched cluster broadcast handler for " + request.getType());
        } else {
            final T parsed;
            try {
                parsed = MessageHelper.parse(ClusterBroadcastHandler.getType(handler), request.getPayload());
            } catch (final InvalidProtocolBufferException e) {
                throw new IllegalStateException(e);
            }
            return handler.handle(parsed);
        }
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClusterRegistry.class);
    
    public static long extractTimeout(final ClusterRequestEnvelope requestEnvelope) {
        return requestEnvelope.getTimeout() > 0 ? requestEnvelope.getTimeout() : Defaults.DEFAULT_TIMEOUT;
    }
    
    private final AsyncFilterChain<ClusterRequestEnvelope, ClusterResponseEnvelope> dispatchRouteChain;
    private final AsyncFilterChain<ClusterBroadcastEnvelope, Boolean> dispatchBroadcastChain;
    protected final AsyncFilterNext<ClusterRequestEnvelope, ClusterResponseEnvelope> handleRouteChain;
    protected final AsyncFilterNext<ClusterBroadcastEnvelope, Boolean> handleBroadcastChain;
    
    private final Map<String, ClusterRouteHandler<?, ?>> routeHandlers = new HashMap<>();
    private final StampedLock routeHandlersLock = new StampedLock();
    private final Map<String, ClusterBroadcastHandler<?>> broadcastHandlers = new HashMap<>();
    private final StampedLock broadcastHandlersLock = new StampedLock();
    
    protected AbstractClusterRegistry(final Set<? extends ClusterDispatchFilterFactory> dispatchFilterFactories,
                                      final Set<? extends ClusterHandleFilterFactory> handleFilterFactories) {
        final List<ClusterDispatchFilterFactory> sortedDispatchFactories = new ArrayList<>(dispatchFilterFactories);
        sortedDispatchFactories.sort(Ordered.COMPARATOR);
        dispatchRouteChain = new AsyncFilterChain<>(
            sortedDispatchFactories.stream()
                .map(ClusterDispatchFilterFactory::createRouteFilter)
                .collect(Collectors.toList()));
        dispatchBroadcastChain = new AsyncFilterChain<>(
            sortedDispatchFactories.stream()
                .map(ClusterDispatchFilterFactory::createBroadcastFilter)
                .collect(Collectors.toList()));
        
        final List<ClusterHandleFilterFactory> sortedHandleFactories = new ArrayList<>(handleFilterFactories);
        final List<ClusterRouteFilter> sortedHandleFilters = sortedHandleFactories.stream()
            .map(ClusterHandleFilterFactory::createRouteFilter)
            .collect(Collectors.toList());
        // add first filter to restore async locals, before anything else
        sortedHandleFilters.add(0, (in, next) -> AsyncLocal.exec(AsyncLocal.snapshot(in.getAsyncLocals()), () -> next.next(in)));
        handleRouteChain = new AsyncFilterChain<>(sortedHandleFilters).with(
            in -> handle(getRouteHandler(in.getType()), in),
            (in, t) -> {
                LOG.error("Unexpected error while handling route", t);
                return result(
                    ClusterResponseEnvelope.newBuilder()
                        .setExceptionClass(t.getClass().getName())
                        .setExceptionMessage(Optional.ofNullable(t.getMessage()).orElse(""))
                        .build());
            });
        final List<ClusterBroadcastFilter> sortedHandleBroadcastFilters = sortedHandleFactories.stream()
            .map(ClusterHandleFilterFactory::createBroadcastFilter)
            .collect(Collectors.toList());
        // add first filter to restore async locals, before anything else
        sortedHandleBroadcastFilters.add(0, (in, next) -> AsyncLocal.exec(AsyncLocal.snapshot(in.getAsyncLocals()), () -> next.next(in)));
        handleBroadcastChain = new AsyncFilterChain<>(sortedHandleBroadcastFilters).with(
            in -> handle(getBroadcastHandler(in.getType()), in),
            (in, t) -> {
                LOG.error("Unexpected error while handling broadcast", t);
                return result(false);
            });
    }
    
    @Override
    public final <REQ extends MessageLite, RES extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                                     final String key,
                                                                                     final REQ request) throws ClusterRouteTimeoutException {
        return route(handler, 0L, key, request);
    }
    
    @Override
    public final <REQ extends MessageLite, RES extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                                     final long key,
                                                                                     final REQ request) throws ClusterRouteTimeoutException {
        return route(handler, key, "", request);
    }
    
    protected abstract <REQ extends MessageLite, RES extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                                           final long id,
                                                                                           final String key,
                                                                                           final REQ request) throws ClusterRouteTimeoutException;
    
    protected final <REQ extends MessageLite, RES extends MessageLite> Async<RES> defaultRoute(final ClusterRouteHandler<REQ, RES> handler,
                                                                                               final long id,
                                                                                               final String key,
                                                                                               final REQ request,
                                                                                               final FunctionThrows<ClusterRequestEnvelope, Async<ClusterResponseEnvelope>, ?> route) throws ClusterRouteTimeoutException {
        return dispatchRouteChain
            .exec(
                ClusterRequestEnvelope.newBuilder()
                    .setType(handler.getKey())
                    .setPayload(request.toByteString())
                    .setId(id)
                    .setKey(key)
                    .setAsyncLocals(AsyncLocal.snapshot().encode())
                    .setTimeout((int) handler.getTimeoutUnit().toMillis(handler.getTimeout()))
                    .build(),
                route,
                (in, t) -> {
                    LOG.error("Unexpected exception while dispatching route", t);
                    return result(
                        ClusterResponseEnvelope.newBuilder()
                            .setExceptionClass(t.getClass().getName())
                            .setExceptionMessage(t.getMessage())
                            .build());
                })
            .map(response -> {
                if (response.getTimeout()) {
                    throw new ClusterRouteTimeoutException();
                } else if (response.getExceptionClass().isEmpty()) {
                    try {
                        return MessageHelper.parse(ClusterRouteHandler.getResponseType(handler), response.getPayload());
                    } catch (final InvalidProtocolBufferException e) {
                        throw new IllegalStateException(e);
                    }
                } else {
                    try {
                        // reconstruct and throw the exception
                        throw sneakyThrow(
                            (Throwable) (response.getExceptionMessage().isEmpty()
                                ? Class.forName(response.getExceptionClass()).getConstructor().newInstance()
                                : Class.forName(response.getExceptionClass())
                                    .getConstructor(String.class)
                                    .newInstance(response.getExceptionMessage())));
                    } catch (final ReflectiveOperationException e) {
                        final IllegalStateException ee = new IllegalStateException(response.getExceptionClass() + response.getExceptionMessage());
                        ee.addSuppressed(e);
                        throw ee;
                    }
                }
            });
    }
    
    @Override
    public final void register(final ClusterRouteHandler<?, ?> handler) {
        LockUtil.write(routeHandlersLock, () -> {
            if (routeHandlers.putIfAbsent(handler.getKey(), handler) != null) {
                throw new IllegalStateException("Route handler with key [" + handler.getKey() + "] already registered");
            }
        });
    }
    
    @Override
    public final void deregister(final ClusterRouteHandler<?, ?> handler) {
        LockUtil.write(routeHandlersLock, () -> {
            if (routeHandlers.remove(handler.getKey()) == null) {
                throw new IllegalStateException("Route handler with key [" + handler.getKey() + "] not registered");
            }
        });
    }
    
    @Override
    public final <T extends MessageLite> Async<Boolean> broadcast(final ClusterBroadcastHandler<T> handler, final T message) {
        return dispatchBroadcastChain.exec(
            ClusterBroadcastEnvelope.newBuilder()
                .setType(handler.getKey())
                .setPayload(message.toByteString())
                .setAsyncLocals(AsyncLocal.snapshot().encode())
                .build(),
            this::broadcast,
            (in, t) -> {
                LOG.error("Unexpected exception while dispatching broadcast", t);
                return result(false);
            });
    }
    
    public abstract Async<Boolean> broadcast(final ClusterBroadcastEnvelope message);
    
    @Override
    public final void register(final ClusterBroadcastHandler<?> handler) {
        LockUtil.write(broadcastHandlersLock, () -> {
            if (broadcastHandlers.putIfAbsent(handler.getKey(), handler) != null) {
                throw new IllegalStateException(
                    "Broadcast handler with key [" + handler.getKey() + "] already registered");
            }
        });
    }
    
    @Override
    public final void deregister(final ClusterBroadcastHandler<?> handler) {
        LockUtil.write(broadcastHandlersLock, () -> {
            if (broadcastHandlers.remove(handler.getKey()) == null) {
                throw new IllegalStateException("Broadcast handler with key [" + handler.getKey() + "] not registered");
            }
        });
    }
    
    private ClusterRouteHandler<? extends MessageLite, ? extends MessageLite> getRouteHandler(final String key) {
        return LockUtil.read(routeHandlersLock, true, () -> routeHandlers.get(key));
    }
    
    private ClusterBroadcastHandler<? extends MessageLite> getBroadcastHandler(final String key) {
        return LockUtil.read(broadcastHandlersLock, true, () -> broadcastHandlers.get(key));
    }
    
}
