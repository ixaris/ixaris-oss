package com.ixaris.commons.microservices.lib.local;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.client.proxy.ListenerKey;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.service.support.ServiceEventDispatcher;

/**
 * This microservices channel is designed to work locally using blocking queues to pass request/response/events from one
 * service to another. For this reason, this channel should only be used together with LocalService since other services
 * would not be able to connect to it.
 *
 * <p>This should only be used in a local lightweight context (e.g. small tests or experiments).
 *
 * @author aldrin.seychell
 */
public final class LocalEvents {
    
    private final ScheduledExecutorService executor;
    
    private final Map<String, Map<ListenerKey, LocalServiceEventHandler>> eventHandlers = new HashMap<>();
    private final Map<String, Map<String, LocalServiceEventDispatcher>> eventDispatchers = new HashMap<>();
    
    public LocalEvents(final ScheduledExecutorService executor) {
        this.executor = executor;
    }
    
    synchronized void createEventHandler(final String name,
                                         final String subscriberName,
                                         final ServicePathHolder path,
                                         final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain) {
        final Map<ListenerKey, LocalServiceEventHandler> handlers = eventHandlers.computeIfAbsent(name, n -> new HashMap<>());
        if (handlers.containsKey(new ListenerKey(subscriberName, path))) {
            throw new IllegalStateException("Event handler for [" + name + "/" + subscriberName + "] already registered");
        }
        final LocalServiceEventHandler handler = new LocalServiceEventHandler(name, path, executor, filterChain);
        handlers.put(new ListenerKey(subscriberName, path), handler);
        
        final Map<String, LocalServiceEventDispatcher> keyDispatchers = eventDispatchers.computeIfAbsent(name, k -> new HashMap<>());
        keyDispatchers.values().forEach(d -> d.addHandlers(Collections.singleton(handler)));
    }
    
    synchronized void destroyEventHandler(
                                          final String name, final String subscriberName, final ServicePathHolder path) {
        final Map<ListenerKey, LocalServiceEventHandler> handlers = eventHandlers.get(name);
        final LocalServiceEventHandler handler = handlers == null ? null : handlers.remove(new ListenerKey(subscriberName, path));
        if (handler == null) {
            throw new IllegalStateException("Event handler for [" + name + "/" + subscriberName + "] not registered");
        }
        if (handlers.isEmpty()) {
            eventHandlers.remove(name);
        }
        
        final Map<String, LocalServiceEventDispatcher> keyDispatchers = eventDispatchers.get(name);
        if (keyDispatchers != null) {
            keyDispatchers.values().forEach(d -> d.removeHandlers(Collections.singleton(handler)));
        }
    }
    
    synchronized ServiceEventDispatcher registerEventDispatcher(final String name, final String key) {
        final Map<String, LocalServiceEventDispatcher> keyDispatchers = eventDispatchers.computeIfAbsent(name, k -> new HashMap<>());
        if (keyDispatchers.containsKey(key)) {
            throw new IllegalStateException("Event dispatcher for [" + name + "/" + key + "] already registered");
        }
        final LocalServiceEventDispatcher dispatcher = new LocalServiceEventDispatcher(name, key);
        keyDispatchers.put(key, dispatcher);
        
        final Map<ListenerKey, LocalServiceEventHandler> handlers = eventHandlers.get(name);
        if (handlers != null) {
            dispatcher.addHandlers(handlers.values());
        }
        return dispatcher;
    }
    
    synchronized void deregisterEventDispatcher(final String name, final String key) {
        final Map<String, LocalServiceEventDispatcher> keyDispatchers = eventDispatchers.get(name);
        if (keyDispatchers != null) {
            final LocalServiceEventDispatcher dispatcher = keyDispatchers.remove(key);
            if (dispatcher == null) {
                throw new IllegalStateException("Event dispatcher for [" + name + "/" + key + "] not registered");
            }
            final Map<ListenerKey, LocalServiceEventHandler> handlers = eventHandlers.get(name);
            if (handlers != null) {
                dispatcher.removeHandlers(handlers.values());
            }
        }
    }
    
}
