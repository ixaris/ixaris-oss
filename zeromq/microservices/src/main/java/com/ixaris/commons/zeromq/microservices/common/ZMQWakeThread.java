package com.ixaris.commons.zeromq.microservices.common;

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.ixaris.commons.zeromq.microservices.ZMQGlobal;
import com.ixaris.commons.zeromq.microservices.ZMQGlobal.ZMQBeforeContextShutdownThread;

/**
 * ZMQ requires all operations to be done from the same thread. This thread wakes such a thread as requires, e.g. to send data, to add a new
 * socket or to update existing sockets. If this is not done, the thread only wakes up when there is data available.
 *
 * @author brian.vella
 */
public final class ZMQWakeThread extends ZMQBeforeContextShutdownThread {
    
    private static final Logger LOG = LoggerFactory.getLogger(ZMQWakeThread.class);
    
    private static final Object WAKE_MARKER = new Object();
    
    private final LinkedBlockingQueue<Object> wakeQueue = new LinkedBlockingQueue<>();
    
    public ZMQWakeThread(final String name) {
        super(name);
    }
    
    @Override
    public void run() {
        
        ZMQ.Socket push = null;
        
        try {
            
            push = ZMQGlobal.getContext().socket(ZMQ.PUSH);
            push.connect("inproc://" + getName());
            
            while (!interrupted()) {
                try {
                    
                    // block on wait queue
                    wakeQueue.take();
                    
                    // consume all wait markers
                    while ((wakeQueue.poll()) != null)
                        ;
                    
                    // wake up
                    final ZMQ.Socket fpush = push;
                    doUninterruptableWork(true, () -> {
                        fpush.send(new byte[] { 0 });
                        return null;
                    });
                    
                } catch (final InterruptedException e) {
                    interrupt();
                }
            }
            
        } catch (final ZMQException e) {
            if (e.getErrorCode() != ZMQ.Error.ETERM.getCode()) {
                LOG.error("error in " + getClass().getName(), e);
                throw e;
            }
            
        } catch (final Exception e) {
            LOG.error("error in " + getClass().getName(), e);
            throw e;
            
        } finally {
            if (push != null) {
                try {
                    push.close();
                } catch (final Exception e) {
                    LOG.warn("error closing push ZMQ socket", e);
                }
                push = null;
            }
        }
    }
    
    public void offerWake() {
        wakeQueue.offer(WAKE_MARKER);
    }
    
}
