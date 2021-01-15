package com.ixaris.commons.zeromq.microservices;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.zeromq.ZMQ;

import com.ixaris.commons.async.lib.thread.InterruptableThread;

public class ZMQGlobal {
    
    public static final String OPERATION_URL_KEY = "OPERATION_URL";
    public static final long REGISTRATION_TIMEOUT = 20L;
    
    private static volatile ZMQ.Context context;
    
    private static final Set<ZMQBeforeContextShutdownThread> ZMQ_BEFORE_SHUTDOWN_SET = new HashSet<>();
    private static final Set<ZMQAfterContextShutdownThread> ZMQ_AFTER_SHUTDOWN_SET = new HashSet<>();
    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);
    
    private ZMQGlobal() {}
    
    public static ZMQ.Context getContext() {
        return context;
    }
    
    public static synchronized void start() {
        if (ACTIVE.compareAndSet(false, true)) {
            context = ZMQ.context(1);
        }
    }
    
    public static synchronized void shutdown() {
        if (ACTIVE.compareAndSet(true, false)) {
            
            // call shutdown on all threads to be shut down before context termination
            // these are threads that do not block on socket operations (typically blocked on queues)
            // ZMQ_BEFORE_SHUTDOWN_SET.forEach(Thread::interrupt);
            for (final ZMQBeforeContextShutdownThread shutdown : ZMQ_BEFORE_SHUTDOWN_SET) {
                shutdown.interrupt();
            }
            try {
                // best effort wait for threads to finish shutting down before terminating the context
                for (final ZMQBeforeContextShutdownThread shutdown : ZMQ_BEFORE_SHUTDOWN_SET) {
                    shutdown.join();
                }
            } catch (final InterruptedException e) {
                // clear interrupted flag and keep shutting down
                Thread.interrupted();
            }
            ZMQ_BEFORE_SHUTDOWN_SET.clear();
            
            // terminate the context. This will close all sockets, even blocked
            context.close();
            
            // call shutdown on all threads to be shut down after context termination
            // these are threads that block on socket operations (typically blocked on poller or socket recv)
            // ZMQ_AFTER_SHUTDOWN_SET.forEach(Thread::interrupt);
            for (final ZMQAfterContextShutdownThread shutdown : ZMQ_AFTER_SHUTDOWN_SET) {
                shutdown.interrupt();
            }
            ZMQ_AFTER_SHUTDOWN_SET.clear();
            
            context = null;
        }
    }
    
    private static synchronized void addBeforeContextShutdown(final ZMQBeforeContextShutdownThread shutdown) {
        if (ACTIVE.get()) {
            ZMQ_BEFORE_SHUTDOWN_SET.add(shutdown);
        } else {
            throw new UnsupportedOperationException("ZMQ not started");
        }
    }
    
    private static synchronized void addAfterContextShutdown(final ZMQAfterContextShutdownThread shutdown) {
        if (ACTIVE.get()) {
            ZMQ_AFTER_SHUTDOWN_SET.add(shutdown);
        } else {
            throw new UnsupportedOperationException("ZMQ not started");
        }
    }
    
    public abstract static class ZMQBeforeContextShutdownThread extends InterruptableThread {
        
        public ZMQBeforeContextShutdownThread(final String name) {
            super(name);
            
            addBeforeContextShutdown(this);
        }
        
    }
    
    public abstract static class ZMQAfterContextShutdownThread extends InterruptableThread {
        
        public ZMQAfterContextShutdownThread(final String name) {
            super(name);
            
            addAfterContextShutdown(this);
        }
        
    }
    
}
