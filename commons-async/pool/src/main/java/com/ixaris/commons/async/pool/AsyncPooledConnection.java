/*
 * Copyright 2002, 2010 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.async.pool;

import com.ixaris.commons.async.pool.AbstractAsyncConnectionPool.ConnectionInfo;
//import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Connections managed by GenericConnectionPool must subclass this abstract class.
 *
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public abstract class AsyncPooledConnection<T, C extends AsyncPooledConnection<T, C>> implements AutoCloseable {
    
    //private static final Cleaner CLEANER = Cleaner.create();
    
    private static <T, C extends AsyncPooledConnection<T, C>> void close(
        final AbstractAsyncConnectionPool<T, C> pool, final AtomicReference<ConnectionInfo<T>> cir
    ) {
        final ConnectionInfo<T> ci = cir.getAndSet(null);
        if (ci != null) {
            pool.releaseConnection(ci);
        }
    }
    
    private final AbstractAsyncConnectionPool<T, C> pool;
    //private final Cleaner.Cleanable cleanable;
    private final AtomicReference<ConnectionInfo<T>> cir;
    
    protected AsyncPooledConnection(final AbstractAsyncConnectionPool<T, C> pool, final ConnectionInfo<T> ci) {
        this.pool = pool;
        this.cir = new AtomicReference<>(ci);
        //final AtomicReference<ConnectionInfo<T>> cir = new AtomicReference<>(ci);
        //this.cleanable = CLEANER.register(this, () -> close(pool, cir));
        //this.cir = cir;
    }
    
    /**
     * @return the real pooled object tied to this particular Connection.
     */
    protected final T getConnection() {
        final ConnectionInfo<T> ci = this.cir.get();
        if (ci == null) {
            throw new IllegalStateException("Connection is closed");
        }
        return ci.connection;
    }
    
    public final boolean isClosed() {
        return cir.get() == null;
    }
    
    /**
     * Releases the connection back to the pool and destroys the wrapper. Use this method when you are done from the
     * connection to avoid starving the pool
     */
    @SuppressWarnings("unchecked")
    public final void close() {
        close(pool, cir);
        //cleanable.clean();
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
}
