/*
 * Copyright 2002, 2010 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.async.pool;

import static com.ixaris.commons.misc.lib.object.Unsafe.UNSAFE;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Connections managed by GenericConnectionPool must subclass this abstract class.
 *  
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public abstract class AsyncPooledConnection<T, C extends AsyncPooledConnection<T, C>> implements AutoCloseable {
    
    private static final long connectionOffset;
    
    static {
        try {
            connectionOffset = UNSAFE.objectFieldOffset(AsyncPooledConnection.class.getDeclaredField("connection"));
        } catch (final Exception ex) {
            throw new Error(ex);
        }
    }
    
    private final AbstractAsyncConnectionPool<T, ?> pool;
    private volatile T connection;
    
    protected AsyncPooledConnection(final AbstractAsyncConnectionPool<T, C> pool, final T connection) {
        this.pool = pool;
        this.connection = connection;
    }
    
    /**
     * @return the real pooled object tied to this particular Connection.
     */
    protected final T getConnection() {
        final T connection = this.connection;
        if (connection == null) {
            throw new IllegalStateException("Connection is closed");
        }
        return connection;
    }
    
    public final boolean isClosed() {
        return connection == null;
    }
    
    /**
     * Releases the connection back to the pool and destroys the wrapper. Use this method when you are done from
     * the connection to avoid your pool starving like ghandi.
     */
    @SuppressWarnings("unchecked")
    public final void close() {
        final T connection = (T) UNSAFE.getAndSetObject(this, connectionOffset, null);
        if (connection != null) {
            pool.releaseConnection(connection);
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
