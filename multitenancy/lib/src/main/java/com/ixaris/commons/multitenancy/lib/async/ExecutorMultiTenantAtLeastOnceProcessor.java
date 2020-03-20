package com.ixaris.commons.multitenancy.lib.async;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.async.ExecutorMultiTenantAtLeastOnceProcessor.Poll.State.PENDING;
import static com.ixaris.commons.multitenancy.lib.async.ExecutorMultiTenantAtLeastOnceProcessor.Poll.State.POLLING;
import static com.ixaris.commons.multitenancy.lib.async.ExecutorMultiTenantAtLeastOnceProcessor.Poll.State.WAITING;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.idempotency.AtLeastOnceMessageType;
import com.ixaris.commons.clustering.lib.idempotency.AtLeastOnceMessageType.NextRetryTimeFunction;
import com.ixaris.commons.clustering.lib.idempotency.PendingMessages;
import com.ixaris.commons.clustering.lib.idempotency.StoredPendingMessage;
import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Implementation of the At-Least-Once processor using Akka actors, while supporting adding/removing of tenants.
 */
public final class ExecutorMultiTenantAtLeastOnceProcessor<T> implements MultiTenantAtLeastOnceProcessor {
    
    private static final class QueueKey {
        
        private int shard;
        private String type;
        
        private QueueKey(final int shard, final String type) {
            this.shard = shard;
            this.type = type;
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, o, other -> (shard == other.shard) && type.equals(other.type));
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(shard, type);
        }
        
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(ExecutorMultiTenantAtLeastOnceProcessor.class);
    
    private final AtLeastOnceMessageType<T> messageType;
    private final long refreshInterval;
    private final ScheduledExecutorService executor;
    
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Set<String> tenants = new HashSet<>();
    private final Map<String, TenantProcessor> processors = new HashMap<>();
    
    ExecutorMultiTenantAtLeastOnceProcessor(final AtLeastOnceMessageType<T> messageType,
                                            final long refreshInterval,
                                            final ScheduledExecutorService executor) {
        this.messageType = messageType;
        this.refreshInterval = refreshInterval;
        this.executor = executor;
    }
    
    @Override
    public synchronized Async<Void> registerTenant(final String tenantId) {
        if (tenants.add(tenantId) && active.get()) {
            startTenant(tenantId);
            LOG.info("Registered tenant: [{}] for {}", tenantId, messageType.getKey());
        }
        return result();
    }
    
    @Override
    public synchronized Async<Void> deregisterTenant(final String tenantId) {
        if (tenants.remove(tenantId) && active.get()) {
            stopTenant(tenantId);
            LOG.info("Deregistered tenant: [{}] for {}", tenantId, messageType.getKey());
        }
        return result();
    }
    
    @Override
    public void pollNow() {
        if (active.get()) {
            final TenantProcessor processor = processors.get(TENANT.get());
            if (processor != null) {
                processor.pollNow();
            }
        }
    }
    
    @Override
    public synchronized void stop() {
        if (active.compareAndSet(true, false)) {
            for (final String tenantId : tenants) {
                stopTenant(tenantId);
            }
        }
    }
    
    private void startTenant(final String tenantId) {
        processors.computeIfAbsent(tenantId, s -> new TenantProcessor(tenantId));
    }
    
    private void stopTenant(final String tenantId) {
        Optional.ofNullable(processors.get(tenantId)).ifPresent(TenantProcessor::stop);
    }
    
    static final class Poll {
        
        enum State {
            WAITING,
            PENDING,
            POLLING
        }
        
        private final boolean active;
        private final State state;
        private final ScheduledFuture<?> schedule;
        
        private Poll(final boolean active, final State state, final ScheduledFuture<?> schedule) {
            this.active = active;
            this.state = state;
            this.schedule = schedule;
        }
        
    }
    
    /**
     * Responsible to fetch from repository (periodically), submits task for processing, and updates repository
     * accordingly.
     */
    final class TenantProcessor {
        
        private final String tenant;
        private final AtomicReference<Poll> poll;
        
        private TenantProcessor(final String tenant) {
            this.tenant = tenant;
            poll = new AtomicReference<>(new Poll(true, WAITING, null));
            poll(true);
        }
        
        public void stop() {
            final Poll prevPoll = poll.getAndSet(new Poll(false, WAITING, null));
            if (prevPoll.schedule != null) {
                prevPoll.schedule.cancel(false);
            }
        }
        
        public void pollNow() {
            poll(true);
        }
        
        public void scheduledPoll() {
            poll(false);
        }
        
        public void poll(final boolean pollAsync) {
            final Poll prevPoll = poll.getAndUpdate(p -> p.active ? new Poll(true, PENDING, null) : p);
            if (prevPoll.active && (prevPoll.state == WAITING)) {
                if (prevPoll.schedule != null) {
                    prevPoll.schedule.cancel(false);
                }
                if (pollAsync) {
                    executor.execute(this::poll);
                } else {
                    poll();
                }
            }
        }
        
        @SuppressWarnings("squid:S1181")
        private Async<Void> poll() {
            return TENANT.exec(tenant, () -> {
                boolean done = false;
                while (!done) {
                    final Poll startPoll = poll.updateAndGet(p -> p.active ? new Poll(true, POLLING, null) : p);
                    if (startPoll.active) {
                        done = await(pollOnce());
                    } else {
                        done = true;
                    }
                }
                
                final ScheduledFuture<?> pollSchedule = executor.schedule(this::scheduledPoll, refreshInterval, TimeUnit.MILLISECONDS);
                final Poll updatedPoll = this.poll.updateAndGet(p -> (p.active && (p.state == WAITING))
                    ? new Poll(true, WAITING, pollSchedule) : p);
                if (updatedPoll.schedule == null) {
                    pollSchedule.cancel(false);
                }
                
                return result();
            });
        }
        
        @SuppressWarnings("squid:S1181")
        private Async<Boolean> pollOnce() {
            final long now = System.currentTimeMillis();
            boolean done;
            try {
                final PendingMessages<T> pending = await(messageType.pending(now));
                if (!pending.getMessages().isEmpty()) {
                    await(processMessages(pending.getMessages(), this::exponentialBackoff));
                }
                done = !pending.isMorePending();
            } catch (final Throwable t) {
                LOG.error("Unexpected error while polling {} for [{}]", messageType.getKey(), tenant, t);
                done = true;
            }
            if (done) {
                final Poll endPoll = poll.updateAndGet(p -> (p.active && (p.state == POLLING)) ? new Poll(true, WAITING, null) : p);
                if (endPoll.active && (endPoll.state == PENDING)) {
                    // something new to process
                    done = false;
                }
            }
            return result(done);
        }
        
        private long exponentialBackoff(final int failureCount) {
            return System.currentTimeMillis() + (refreshInterval * failureCount * failureCount) - (refreshInterval / 2);
        }
        
        private Async<Void> processMessages(final List<StoredPendingMessage<T>> messages,
                                            final NextRetryTimeFunction nextRetryTimeFunction) {
            return all(messages.stream()
                .collect(Collectors.groupingBy(m -> new QueueKey(m.getShard(), m.getMessageSubType()),
                    Collectors.toList()))
                .values()
                .stream()
                .map(m -> processKeyMessages(m, nextRetryTimeFunction))
                .collect(Collectors.toList()))
                    .map(r -> null);
        }
        
        private Async<Void> processKeyMessages(final List<StoredPendingMessage<T>> messages,
                                               final NextRetryTimeFunction nextRetryTimeFunction) {
            for (final StoredPendingMessage<T> message : messages) {
                LOG.debug("Processing {} of type {}", message, messageType.getKey());
                try {
                    await(messageType.processMessage(message, nextRetryTimeFunction));
                    LOG.debug("Done processing {} of type {}", message, messageType.getKey());
                } catch (final RuntimeException e) {
                    LOG.error("Error processing {} of type {}", message, messageType.getKey(), e);
                    if (messageType.isFailedMessageBlocksQueue()) {
                        return result();
                    }
                }
            }
            return result();
        }
        
    }
    
}
