/*
 * Copyright 2002, 2010 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.async.pool;

/**
 * Connections managed by GenericConnectionPool must subclass this abstract class.
 *  
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public abstract class AsyncPooledConnection<T, C extends AsyncPooledConnection<T, C>> implements AutoCloseable {
    
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
    public final void close() {
        if (connection != null) {
            pool.releaseConnection(connection);
            connection = null;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
