/*
 * Copyright 2002, 2010 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.async.pool;

import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.scheduler.Scheduler;
import com.ixaris.commons.async.pool.TestConnectionPool.TestConnection;
import com.ixaris.commons.async.pool.TestConnectionPool.TestPooledConnection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TestConnectionPool extends AbstractAsyncConnectionPool<TestPooledConnection, TestConnection> {
    
    static final class TestConnection extends AsyncPooledConnection<TestPooledConnection, TestConnection> {
        
        TestConnection(final TestConnectionPool pool, final ConnectionInfo<TestPooledConnection> conn) {
            
            super(pool, conn);
        }
        
    }
    
    // counters for testing
    long connectionsCreated = 0L;
    long connectionsClosed = 0L;
    long connectionsServiced = 0L;
    long firstService = 0L;
    
    TestConnectionPool() {
        super(5, 10, 2500L, 200L);
    }
    
    @Override
    public Async<TestPooledConnection> createPooledConnection() {
        return result(new TestPooledConnection());
    }
    
    @Override
    public TestConnection wrapConnection(final ConnectionInfo<TestPooledConnection> conn) {
        return new TestConnection(this, conn);
    }
    
    @Override
    public Async<TestPooledConnection> serviceConnection(final TestPooledConnection pooledConn) {
        if (firstService == 0L) {
            firstService = System.currentTimeMillis();
        }
        connectionsServiced++;
        return result(pooledConn);
    }
    
    @Override
    protected ScheduledFuture<?> scheduleServiceTask(
        final Runnable task, final long initialDelay, final long delay, final TimeUnit unit
    ) {
        return Scheduler.commonScheduler().scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }
    
    @Override
    protected boolean isClosed(final TestPooledConnection connection) {
        return connection.closed;
    }
    
    @Override
    protected void close(final TestPooledConnection connection) {
        connection.close();
    }
    
    final class TestPooledConnection {
        
        private boolean closed = false;
        
        TestPooledConnection() {
            connectionsCreated++;
        }
        
        void close() {
            connectionsClosed++;
            closed = true;
        }
        
    }
    
}
