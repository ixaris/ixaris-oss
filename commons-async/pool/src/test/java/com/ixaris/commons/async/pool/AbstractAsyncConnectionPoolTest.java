/*
 * Copyright 2002, 2010 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.async.pool;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.junit.Ignore;
import org.junit.Test;

import com.ixaris.commons.async.pool.TestConnectionPool.TestConnection;

public class AbstractAsyncConnectionPoolTest {
    
    @Test
    public void testGetConnections() throws Throwable {
        
        final TestConnectionPool connPool = new TestConnectionPool();
        connPool.start();
        
        // get 4 connections
        final LinkedList<TestConnection> connections = new LinkedList<>();
        for (int i = 0; i < 4; i++) {
            connPool.getConnection(10L).thenAccept(connections::add);
        }
        
        assertEquals(5, connPool.connectionsCreated); // Minimum (5) created
        
        // now we have 7 in pool
        for (int i = 4; i < 7; i++) {
            connPool.getConnection(10L).thenAccept(connections::add);
        }
        
        assertEquals(7, connPool.connectionsCreated);
        assertEquals(0, connPool.connectionsClosed);
        
        // release them all
        connections.forEach(AsyncPooledConnection::close);
        connections.clear();
        
        Thread.sleep(2800L);  // Wait for maxIdleTime & cleanup
        assertEquals(7, connPool.connectionsClosed);
        assertEquals(7 + 5, connPool.connectionsCreated);
        
        // try getting more than 10 connections
        for (int i = 0; i < 10; i++) {
            connPool.getConnection(10L).thenAccept(connections::add);
        }
        
        try {
            block(connPool.getConnection(10L));
            fail("Should throw ExecutionException caused by ConnectionAcquisitionException");
        } catch (final ConnectionAcquisitionException e) {
            // expected
        }
        
        // try to get a connection and release before timeout, should not fail
        final CompletionStage<TestConnection> p2 = connPool.getConnection(1000L);
        connections.removeLast().close();
        p2.toCompletableFuture().join();
        
        connPool.stop();
    }
    
    @Test
    @Ignore
    // this test is prone to failing because of timing
    public void testServiceScheduler() throws InterruptedException {
        
        final TestConnectionPool connPool = new TestConnectionPool();
        
        connPool.start();
        
        Thread.sleep(250L);
        
        long servicedSoFar = connPool.connectionsServiced;
        Thread.sleep(850L - System.currentTimeMillis() + connPool.firstService); // wait for 4 serviceIntervals + bit more to be sure
        // min 5 connections, serviced 4 times => 20
        System.out.println(connPool.connectionsServiced - servicedSoFar);
        assertTrue((connPool.connectionsServiced - servicedSoFar >= 15) && (connPool.connectionsServiced - servicedSoFar <= 20));
        
        final List<TestConnection> connections = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            connPool.getConnection(10L).thenAccept(connections::add);
        }
        connections.forEach(AsyncPooledConnection::close);
        
        servicedSoFar = connPool.connectionsServiced;
        Thread.sleep(1650L - System.currentTimeMillis() + connPool.firstService); // wait for 4 serviceIntervals + bit more to be sure
        // 10 connections, serviced 4 times => 40
        System.out.println(connPool.connectionsServiced - servicedSoFar);
        assertTrue((connPool.connectionsServiced - servicedSoFar >= 30) && (connPool.connectionsServiced - servicedSoFar <= 40));
        
        for (int i = 0; i < 2; i++) {
            connPool.getConnection(10L).thenAccept(connections::add);
        }
        
        servicedSoFar = connPool.connectionsServiced;
        Thread.sleep(2450L - System.currentTimeMillis() + connPool.firstService); // wait for 4 serviceIntervals + bit more to be sure
        // 8 connections, serviced every 4 times => 32
        System.out.println(connPool.connectionsServiced - servicedSoFar);
        assertTrue((connPool.connectionsServiced - servicedSoFar >= 24) && (connPool.connectionsServiced - servicedSoFar <= 32));
        
        connPool.stop();
    }
    
    @Test
    public void testPerformance() {
        
        final TestConnectionPool connPool = new TestConnectionPool();
        connPool.start();
        
        final List<TestConnection> connections = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            connPool.getConnection(10L).thenAccept(connections::add);
        }
        connections.forEach(TestConnection::close);
        connections.clear();
        
        final long start = System.currentTimeMillis();
        for (int j = 0; j < 1000; j++) {
            for (int i = 0; i < 10; i++) {
                connPool.getConnection(10L).thenAccept(connections::add);
            }
            connections.forEach(TestConnection::close);
            connections.clear();
        }
        
        long time = (System.currentTimeMillis() - start);
        // assertTrue(time < 250);
        System.out.println("10000 took " + time);
        
        connPool.stop();
    }
    
    @Test
    public void testStartStop() {
        
        final TestConnectionPool connPool = new TestConnectionPool();
        connPool.start();
        
        // get 3 connections
        final List<TestConnection> connections = new LinkedList<>();
        for (int i = 0; i < 3; i++) {
            connPool.getConnection(10L).thenAccept(connections::add);
        }
        assertEquals(5, connPool.connectionsCreated); // Minimum (5) created
        
        connPool.stop();
        
        assertEquals(5, connPool.connectionsCreated);
        assertEquals(2, connPool.connectionsClosed);
        
        for (TestConnection c : connections) {
            assertFalse(c.isClosed());
            c.close();
        }
        
        assertEquals(5, connPool.connectionsClosed);
    }
    
    @Test
    public void testStartStopFinalisation() {
        
        final TestConnectionPool connPool = new TestConnectionPool();
        connPool.start();
        
        // get 3 connections
        for (int i = 0; i < 3; i++) {
            connPool.getConnection(10L);
        }
        assertEquals(5, connPool.connectionsCreated); // Minimum (5) created
        
        connPool.stop();
        
        assertEquals(5, connPool.connectionsCreated);
        assertEquals(2, connPool.connectionsClosed);
        
        // try to get gc to collect connections
        for (int i = 0; i < 1000000; i++) {
            new Object() {
                
                private byte[] x = new byte[1000];
            };
        }
        
        assertEquals(5, connPool.connectionsClosed);
        
    }
    
}
