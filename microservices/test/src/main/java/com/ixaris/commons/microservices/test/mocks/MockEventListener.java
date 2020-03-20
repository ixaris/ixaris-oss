package com.ixaris.commons.microservices.test.mocks;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceHeader;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class MockEventListener<C extends MessageLite, E extends MessageLite> implements ServiceEventListener<C, E> {
    
    private static final int DEFAULT_TIMEOUT = 10;
    
    private static final Logger LOG = LoggerFactory.getLogger(MockEventListener.class);
    
    private final Map<Tuple2<String, Long>, BlockingQueue<Tuple2<ServiceEventHeader<C>, E>>> eventFutures = new HashMap<>();
    
    public BlockingQueue<Tuple2<ServiceEventHeader<C>, E>> get(final ServiceHeader<?> header) {
        synchronized (eventFutures) {
            return eventFutures.computeIfAbsent(extractIntent(header), k -> new LinkedBlockingQueue<>());
        }
    }
    
    public final Tuple2<ServiceEventHeader<C>, E> expectOne(final ServiceHeader<?> header) throws InterruptedException {
        return expectOne(header, DEFAULT_TIMEOUT, TimeUnit.SECONDS);
    }
    
    public final Tuple2<ServiceEventHeader<C>, E> expectOne(final ServiceHeader<?> header, final long timeout, final TimeUnit unit) throws InterruptedException {
        final BlockingQueue<Tuple2<ServiceEventHeader<C>, E>> queue = get(header);
        final Tuple2<ServiceEventHeader<C>, E> tuple = queue.poll(timeout, unit);
        if (tuple == null) {
            throw new EventNotDelivered();
        } else {
            return tuple;
        }
    }
    
    public final void expectNone(final ServiceHeader<?> header) throws InterruptedException {
        expectNone(header, DEFAULT_TIMEOUT, TimeUnit.SECONDS);
    }
    
    public final void expectNone(final ServiceHeader<?> header, final long timeout, final TimeUnit unit) throws InterruptedException {
        final BlockingQueue<Tuple2<ServiceEventHeader<C>, E>> queue = get(header);
        final Tuple2<ServiceEventHeader<C>, E> tuple = queue.poll(timeout, unit);
        if (tuple != null) {
            throw new UnexpectedEventDelivered();
        }
    }
    
    @Override
    public Async<Void> onEvent(final ServiceEventHeader<C> header, final E event) {
        
        LOG.info("Listener Received event {} with header {}", event, header);
        synchronized (eventFutures) {
            final BlockingQueue<Tuple2<ServiceEventHeader<C>, E>> queue = eventFutures.computeIfAbsent(extractIntent(header), k -> new LinkedBlockingQueue<>());
            queue.offer(tuple(header, event));
        }
        return result();
    }
    
    public static class EventNotDelivered extends RuntimeException {}
    
    public static class UnexpectedEventDelivered extends RuntimeException {}
    
    private Tuple2<String, Long> extractIntent(final ServiceHeader<?> header) {
        return tuple(header.getTenantId(), header.getIntentId());
    }
    
}
