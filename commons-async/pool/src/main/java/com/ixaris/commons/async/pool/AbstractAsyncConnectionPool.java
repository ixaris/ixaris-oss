/*
 * Copyright 2002, 2010 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.async.pool;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncExecutor.schedule;
import static com.ixaris.commons.async.lib.CompletableFutureUtil.completeFrom;
import static com.ixaris.commons.async.lib.CompletableFutureUtil.reject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.FutureAsync;

/**
 * An abstract Connection Pool, managing a number of connections.
 *
 * A concrete subclass should:
 * <ul>
 * <li>Call start() to initiate and start connection pool.</li>
 * <li>Call stop() to close all connections and stop pool.</li>
 * <li>Every serviceInterval, serviceConnection() is called on connections and pool size is adjusted to at least minSize</li>
 * </ul>
 */
public abstract class AbstractAsyncConnectionPool<T, C extends AsyncPooledConnection<T, C>> implements AsyncConnectionPool<C> {
    
    private static final int INITIALISED = 0;
    private static final int ACTIVE = 1;
    private static final int STOPPED = 2;
    
    private final AtomicInteger state = new AtomicInteger(INITIALISED);
    private final AtomicInteger count = new AtomicInteger(0); // count the number of object in the pool
    
    private ConcurrentHashMap<T, ConnectionInfo<T>> inUse;
    private ConcurrentLinkedQueue<ConnectionInfo<T>> available;
    private ConcurrentLinkedQueue<AtomicReference<FutureAsync<C>>> waiting;
    
    // configurable params
    private int minSize = 2; // minimum size of the pool, default 2
    private int maxSize = 50; // maximum size of the pool, if set to 0 : unlimited, default 50
    private long maxIdleTime = 900000L; // lifetime of an object in the pool, default 15 minutes
    private long serviceInterval = 300000L; // interval for the service task, default 5 minutes
    
    private ScheduledFuture<?> serviceTask;
    
    /**
     * Creates an GenericConnectionPool with the default params.
     */
    public AbstractAsyncConnectionPool() {}
    
    /**
     * Creates an GenericConnectionPool with min/max size and default params.
     */
    public AbstractAsyncConnectionPool(final int minSize, final int maxSize) {
        setSizeRange(minSize, maxSize);
    }
    
    /**
     * Constructor configuring all properties
     * 
     * @param minSize minimum size of the pool, default 2
     * @param maxSize maximum size of the pool, if set to 0 : unlimited, default 50
     * @param maxIdleTime lifetime of an object in the pool, default 10 minutes
     * @param serviceInterval interval for the service task, default 5 minutes
     */
    public AbstractAsyncConnectionPool(final int minSize, final int maxSize, final long maxIdleTime, final long serviceInterval) {
        this(minSize, maxSize);
        
        setMaxIdleTime(maxIdleTime);
        setServiceInterval(serviceInterval);
    }
    
    /**
     * Create a new pooled connection
     */
    public abstract Async<T> createPooledConnection();
    
    /**
     * Create a new connection
     */
    public abstract C wrapConnection(T pooledConn);
    
    /**
     * Service the connection (called periodically) e.g. for Keep Alive and check whether the connection is still useable
     * 
     * @param pooledConn the connection to service
     * @return promise fulfilled when the connection is services
     */
    public abstract Async<T> serviceConnection(T pooledConn);
    
    /**
     * Concrete Connection Pools (subclasses) can place initialisation code, executed before start()
     */
    protected void preStart() {}
    
    /**
     * Concrete Connection Pools (subclasses) can place cleanup code, executed after stop()
     */
    protected void postStop() {}
    
    public final void setSizeRange(final int minSize, final int maxSize) {
        if (state.get() != INITIALISED) {
            throw new IllegalStateException("Pool has already been started");
        }
        
        if (minSize < 0) {
            throw new IllegalArgumentException("Invalid min size (<0): " + minSize);
        }
        if (maxSize < 1) {
            throw new IllegalArgumentException("Invalid max size (<1): " + maxSize);
        }
        if (maxSize < minSize) {
            throw new IllegalArgumentException("Invalid size range (max<min): " + minSize + " - " + maxSize);
        }
        this.minSize = minSize;
        this.maxSize = maxSize;
    }
    
    public final void setMaxIdleTime(final long maxIdleTime) {
        if (state.get() != INITIALISED) {
            throw new IllegalStateException("Pool has already been started");
        }
        
        this.maxIdleTime = maxIdleTime;
    }
    
    public final void setServiceInterval(final long serviceInterval) {
        if (state.get() != INITIALISED) {
            throw new IllegalStateException("Pool has already been started");
        }
        
        this.serviceInterval = serviceInterval;
    }
    
    /**
     * Initialise the Connection Pool
     */
    public final void start() {
        if (state.compareAndSet(INITIALISED, ACTIVE)) {
            preStart();
            
            inUse = new ConcurrentHashMap<>(maxSize);
            available = new ConcurrentLinkedQueue<>();
            waiting = new ConcurrentLinkedQueue<>();
            
            createMinSize();
            
            // set up timer
            serviceTask = scheduleWithFixedDelay(this::service, serviceInterval, serviceInterval, TimeUnit.MILLISECONDS);
        } else {
            throw new IllegalStateException("Pool has already been started");
        }
    }
    
    protected abstract ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, final long delay, final TimeUnit unit);
    
    /**
     * switch off the pool
     */
    public final void stop() {
        if (state.compareAndSet(ACTIVE, STOPPED)) {
            if (serviceTask != null) {
                serviceTask.cancel(false);
                serviceTask = null;
            }
            
            closeAvailableConnections();
            postStop();
        } else {
            throw new IllegalStateException("Pool is inactive");
        }
    }
    
    @Override
    public final boolean isActive() {
        return state.get() == ACTIVE;
    }
    
    /**
     * @param timeout the timeout until the promise is rejected
     * @return a promise fulfilled when a connection is available
     */
    @Override
    public final Async<C> getConnection(final long timeout) {
        if (!isActive()) {
            throw new IllegalStateException("Pool is inactive");
        }
        
        createMinSize();
        
        final ConnectionInfo<T> ci = available.poll();
        if (ci != null) {
            // can acquire immediately
            return acquireConnection(ci);
            
        } else {
            // we try to create another connection
            int c;
            while ((c = count.get()) < maxSize) {
                if (count.compareAndSet(c, ++c)) {
                    createConnection();
                    break;
                }
            }
            
            // and wait
            final FutureAsync<C> future = new FutureAsync<>();
            final AtomicReference<FutureAsync<C>> ref = new AtomicReference<>(future);
            waiting.offer(ref);
            
            if (timeout > 0) {
                try {
                    schedule(timeout, TimeUnit.MILLISECONDS, () -> {
                        final FutureAsync<C> cc = ref.getAndSet(null);
                        if (cc != null) {
                            reject(future, new ConnectionAcquisitionException("Could not obtain a connection after " + timeout + " ms (" + count + " in use)"));
                        }
                    });
                } catch (final IllegalStateException e) {
                    // this can happen if concurrently, pool is stopped, so ignore
                }
            }
            
            checkWaiting();
            return future;
        }
    }
    
    protected abstract boolean isClosed(final T connection);
    
    protected abstract void close(final T connection);
    
    protected Async<T> onConnectionObtained(final T connection) {
        return result(connection);
    }
    
    protected Async<T> onConnectionReleased(final T connection) {
        return result(connection);
    }
    
    final void releaseConnection(final T connection) {
        final ConnectionInfo<T> ci = inUse.remove(connection);
        
        if ((ci != null) && !isClosed(connection)) {
            release(() -> onConnectionReleased(connection), ci, false);
        } else {
            count.decrementAndGet();
            if (isActive()) {
                createMinSize();
            }
        }
    }
    
    private void closeInternal(final T connection) {
        try {
            close(connection);
        } catch (final Exception e) {
            // log
        }
    }
    
    private void createMinSize() {
        int c;
        while ((c = count.get()) < minSize) {
            if (count.compareAndSet(c, ++c)) {
                createConnection();
            }
        }
    }
    
    private Async<Void> createConnection() {
        try {
            final T c = await(createPooledConnection());
            if (isActive()) {
                available.offer(new ConnectionInfo<>(c, System.currentTimeMillis()));
                checkWaiting();
            } else {
                count.decrementAndGet();
                closeInternal(c);
            }
        } catch (final Throwable t) {
            count.decrementAndGet();
            if (isActive()) {
                createMinSize();
            }
        }
        return result();
    }
    
    private void checkWaiting() {
        if (!waiting.isEmpty() && !available.isEmpty()) {
            ConnectionInfo<T> ci;
            while ((ci = available.poll()) != null) {
                boolean used = false;
                AtomicReference<FutureAsync<C>> ref;
                while ((ref = waiting.poll()) != null) {
                    
                    final FutureAsync<C> future = ref.getAndSet(null);
                    if (future != null) {
                        completeFrom(future, acquireConnection(ci));
                        // connection used, so break out of the while loop and get another connection
                        used = true;
                        break;
                    }
                }
                
                if (!used) {
                    // did not find a waiting callback for this connection, so return it to the queue
                    available.offer(ci);
                    break;
                }
            }
        }
        
        if (!isActive()) {
            closeAvailableConnections();
        }
    }
    
    private Async<C> acquireConnection(final ConnectionInfo<T> ci) {
        await(onConnectionObtained(ci.connection));
        try {
            inUse.put(ci.connection, ci);
            return result(wrapConnection(ci.connection));
        } catch (final RuntimeException e) {
            count.decrementAndGet();
            closeInternal(ci.connection);
            
            if (isActive()) {
                createMinSize();
            }
            throw e;
        }
    }
    
    private void closeAvailableConnections() {
        ConnectionInfo<T> c;
        while ((c = available.poll()) != null) {
            // Best effort to close the connection. Not much we can do at this point if there is an exception
            closeInternal(c.connection);
        }
    }
    
    private void service() {
        if (!isActive()) {
            return;
        }
        
        final long now = System.currentTimeMillis();
        final long maxIdleDeadline = now - maxIdleTime;
        final long serviceDeadline = now - serviceInterval;
        
        int i = count.get();
        
        ConnectionInfo<T> ci;
        while ((i > 0) && isActive() && (ci = available.poll()) != null) {
            i--;
            
            if (ci.lastUse <= maxIdleDeadline) {
                // exceeded maxidletime
                count.decrementAndGet();
                closeInternal(ci.connection);
                if (isActive()) {
                    createMinSize();
                }
                
            } else if (isActive()) {
                if (ci.lastAction <= serviceDeadline) {
                    final T connection = ci.connection;
                    release(() -> serviceConnection(connection), ci, true);
                } else {
                    // return to end queue
                    available.offer(ci);
                    checkWaiting();
                }
                
            } else {
                count.decrementAndGet();
                closeInternal(ci.connection);
            }
        }
        
    }
    
    private Async<Void> release(final Supplier<Async<T>> supplier,
                                final ConnectionInfo<T> ci,
                                final boolean service) {
        try {
            await(supplier.get());
            if (isActive()) {
                ci.lastAction = System.currentTimeMillis();
                if (!service) {
                    ci.lastUse = System.currentTimeMillis();
                }
                available.offer(ci);
                checkWaiting();
            } else {
                count.decrementAndGet();
                closeInternal(ci.connection);
            }
        } catch (final Throwable t) { // NOSONAR
            count.decrementAndGet();
            closeInternal(ci.connection);
            if (isActive()) {
                createMinSize();
            }
        }
        return result();
    }
    
    private static class ConnectionInfo<T> {
        
        private final T connection;
        private long lastAction;
        private long lastUse;
        
        private ConnectionInfo(final T connection, final long timestamp) {
            
            this.connection = connection;
            this.lastAction = timestamp;
            this.lastUse = timestamp;
        }
        
    }
    
}
