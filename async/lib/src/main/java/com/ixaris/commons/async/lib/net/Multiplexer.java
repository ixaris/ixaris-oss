package com.ixaris.commons.async.lib.net;

import static com.ixaris.commons.async.lib.CompletableFutureUtil.reject;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncTrace;
import com.ixaris.commons.async.lib.FutureAsync;

public final class Multiplexer {
    
    private final Selector selector;
    private final ConcurrentLinkedDeque<Registration> pendingRegistrations = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Deregistration> pendingDeregistrations = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<OpsUpdate> pendingOpsUpdates = new ConcurrentLinkedDeque<>();
    
    public Multiplexer() {
        try {
            selector = SelectorProvider.provider().openSelector();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public int size() {
        return selector.keys().size();
    }
    
    public void wakeup() {
        selector.wakeup();
    }
    
    public void select() {
        try {
            int select = selector.select();
            updateOps();
            register();
            deregister();
            
            if (select > 0) {
                process();
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public Async<SelectionKey> register(final SelectableChannel channel, final int ops, final ChannelProcessor attachment) {
        final Registration registration = new Registration(channel, ops, attachment);
        pendingRegistrations.offer(registration);
        selector.wakeup();
        return registration.future;
    }
    
    public Async<SelectionKey> deregister(final SelectionKey key) {
        final Deregistration deregistration = new Deregistration(key);
        pendingDeregistrations.offer(deregistration);
        selector.wakeup();
        return deregistration.future;
    }
    
    public Async<SelectionKey> updateOps(final SelectionKey key, final int ops) {
        final OpsUpdate opsUpdate = new OpsUpdate(key, ops);
        pendingOpsUpdates.offer(opsUpdate);
        selector.wakeup();
        return opsUpdate.future;
    }
    
    private void process() {
        final Iterator<SelectionKey> i = selector.selectedKeys().iterator();
        while (i.hasNext()) {
            final SelectionKey key = i.next();
            i.remove();
            
            if (!key.isValid()) {
                continue;
            }
            
            final ChannelProcessor channelProcessor = (ChannelProcessor) key.attachment();
            if (key.isReadable()) {
                channelProcessor.read(key);
            }
            if (key.isWritable()) {
                channelProcessor.write(key);
            }
            if (key.isConnectable()) {
                channelProcessor.connect(key);
            }
            if (key.isAcceptable()) {
                channelProcessor.accept(key);
            }
        }
    }
    
    private void register() {
        Registration registration;
        while ((registration = pendingRegistrations.poll()) != null) {
            final Registration r = registration;
            AsyncTrace.exec(r.trace, () -> {
                try {
                    final SelectionKey key = r.channel.register(selector, r.ops, r.attachment);
                    r.future.complete(key);
                } catch (final ClosedChannelException | RuntimeException e) {
                    reject(r.future, e);
                }
            });
        }
    }
    
    private void deregister() {
        Deregistration deregistration;
        while ((deregistration = pendingDeregistrations.poll()) != null) {
            final Deregistration d = deregistration;
            AsyncTrace.exec(d.trace, () -> {
                try {
                    d.key.cancel();
                    d.future.complete(d.key);
                } catch (final RuntimeException e) {
                    reject(d.future, e);
                }
            });
        }
    }
    
    private void updateOps() {
        OpsUpdate opsUpdate;
        while ((opsUpdate = pendingOpsUpdates.poll()) != null) {
            final OpsUpdate o = opsUpdate;
            AsyncTrace.exec(o.trace, () -> {
                try {
                    o.key.interestOps(o.ops);
                    o.future.complete(o.key);
                } catch (final RuntimeException e) {
                    reject(o.future, e);
                }
            });
        }
    }
    
    private abstract static class Operation {
        
        final FutureAsync<SelectionKey> future = new FutureAsync<>();
        final AsyncTrace trace = AsyncTrace.get();
        
    }
    
    private static final class Registration extends Operation {
        
        private final SelectableChannel channel;
        private final int ops;
        private final ChannelProcessor attachment;
        
        private Registration(final SelectableChannel channel, final int ops, final ChannelProcessor attachment) {
            this.channel = channel;
            this.ops = ops;
            this.attachment = attachment;
        }
    }
    
    private static final class Deregistration extends Operation {
        
        private final SelectionKey key;
        
        private Deregistration(final SelectionKey key) {
            this.key = key;
        }
    }
    
    private static final class OpsUpdate extends Operation {
        
        private final SelectionKey key;
        private final int ops;
        
        private OpsUpdate(final SelectionKey key, final int ops) {
            this.key = key;
            this.ops = ops;
        }
    }
    
}
