package com.ixaris.commons.kafka.multitenancy;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.multitenancy.lib.object.AbstractEagerMultiTenantSharedObject;

/**
 * Class used for managing kafka connections over multiple tenants
 *
 * @author <a href="mailto:Armand.Sciberras@ixaris.com">Armand.Sciberras</a>
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class KafkaMultiTenantConnection extends AbstractEagerMultiTenantSharedObject<KafkaConnectionHandler, KafkaConnectionHandler, Void> implements KafkaConnectionHandler {
    
    private final AtomicInteger active = new AtomicInteger();
    private final KafkaConnection kafkaConnection;
    
    public KafkaMultiTenantConnection(final KafkaConnection kafkaConnection) {
        super(KafkaMultiTenantConnection.class.getSimpleName());
        this.kafkaConnection = kafkaConnection;
    }
    
    @Override
    public Async<Void> preActivate(final String tenantId) {
        return result();
    }
    
    @Override
    public Async<Void> activate(final String tenantId) {
        addTenant(tenantId, null);
        return result();
    }
    
    @Override
    public Async<Void> deactivate(final String tenantId) {
        removeTenant(tenantId);
        return result();
    }
    
    @Override
    public Async<Void> postDeactivate(final String tenantId) {
        return result();
    }
    
    @Override
    protected String computeHash(final Void create) {
        return "";
    }
    
    @Override
    protected KafkaConnectionHandler createShared(final Void create) {
        start();
        return this;
    }
    
    @Override
    protected KafkaConnectionHandler wrap(final KafkaConnectionHandler instance, final String tenantId) {
        return instance;
    }
    
    @Override
    protected void destroyShared(final KafkaConnectionHandler instance) {
        stop();
    }
    
    public void start() {
        if (active.getAndIncrement() == 0) {
            kafkaConnection.start();
        }
    }
    
    public void stop() {
        if (active.decrementAndGet() == 0) {
            kafkaConnection.stop();
        }
    }
    
    @Override
    public void subscribe(final String subscriberName, final String eventName, final KafkaMessageHandler messageHandler) {
        // Current approach is to use the same topic for all tenants, hence we just need to subscribe for the topic.
        // If we consider again to use separate topics for different tenants, we can change this implementation to
        // prefix the tenant in the event name
        kafkaConnection.subscribe(subscriberName, eventName, messageHandler);
    }
    
    @Override
    public void unsubscribe(final String subscriberName, final String eventName) {
        kafkaConnection.unsubscribe(subscriberName, eventName);
    }
    
    @Override
    public void register(final Set<String> topics) {
        kafkaConnection.register(topics);
    }
    
    @Override
    public Async<Void> publish(final String eventName, final long partitionId, final byte[] message) {
        return kafkaConnection.publish(eventName, partitionId, message);
    }
    
}
