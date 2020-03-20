package com.ixaris.commons.microservices.lib.local;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.StampedLock;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.misc.lib.lock.LockUtil;

public final class LocalOperations {
    
    private final ScheduledExecutorService executor;
    
    private final Map<String, LocalServiceOperationDispatcher> operationDispatchers = new HashMap<>();
    private final Map<String, Map<String, LocalServiceOperationHandler>> operationHandlers = new HashMap<>();
    private final StampedLock lock = new StampedLock();
    
    public LocalOperations(final ScheduledExecutorService executor) {
        this.executor = executor;
    }
    
    public LocalServiceOperationDispatcher getOperationDispatcher(final String name) {
        return LockUtil.write(lock, () -> {
            final LocalServiceOperationDispatcher dispatcher;
            if (operationDispatchers.containsKey(name)) {
                throw new IllegalStateException("Operation dispatcher for [" + name + "] already registered");
            }
            
            dispatcher = new LocalServiceOperationDispatcher(name, executor, this);
            operationDispatchers.put(name, dispatcher);
            
            final Map<String, LocalServiceOperationHandler> keyHandlers = operationHandlers.get(name);
            if (keyHandlers != null) {
                keyHandlers.forEach(dispatcher::setHandler);
            }
            return dispatcher;
        });
    }
    
    public LocalServiceOperationHandler registerOperationHandler(final String name,
                                                                 final String key,
                                                                 final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain) {
        return LockUtil.write(lock, () -> {
            final Map<String, LocalServiceOperationHandler> keyHandlers = operationHandlers.computeIfAbsent(name, k -> new HashMap<>());
            if (keyHandlers.containsKey(key)) {
                throw new IllegalStateException("Operation handler for [" + name + "/" + key + "] already registered");
            }
            final LocalServiceOperationHandler handler = new LocalServiceOperationHandler(name, key, filterChain);
            operationHandlers.computeIfAbsent(name, k -> new HashMap<>()).put(key, handler);
            final LocalServiceOperationDispatcher dispatcher = operationDispatchers.get(name);
            if (dispatcher != null) {
                dispatcher.setHandler(key, handler);
            }
            return handler;
        });
    }
    
    public void deregisterOperationHandler(final String name, final String key) {
        LockUtil.write(lock, () -> {
            final Map<String, LocalServiceOperationHandler> keyHandlers = operationHandlers.get(name);
            if (keyHandlers != null) {
                final LocalServiceOperationHandler handler = keyHandlers.remove(key);
                if (handler == null) {
                    throw new IllegalStateException("Operation handler for [" + name + "/" + key + "] not registered");
                }
            }
            
            final LocalServiceOperationDispatcher dispatcher = operationDispatchers.get(name);
            if (dispatcher != null) {
                dispatcher.unsetHandler(key);
            }
        });
    }
    
    boolean isHandlerRegistered(final String name, final String key) {
        return LockUtil.read(lock, true, () -> Optional.ofNullable(operationHandlers.get(name)).map(keys -> keys.containsKey(key)).orElse(false));
    }
    
}
