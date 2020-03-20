package com.ixaris.commons.multitenancy.lib.async;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.idempotency.AtLeastOnceMessageType;
import com.ixaris.commons.clustering.lib.idempotency.AtLeastOnceProcessor;
import com.ixaris.commons.multitenancy.lib.TenantLifecycleParticipant;

public abstract class AbstractTenantAwareAtLeastOnceMessageType<T> implements AtLeastOnceMessageType<T>, TenantLifecycleParticipant {
    
    private final MultiTenantAtLeastOnceProcessorFactory processorFactory;
    private final long refreshInterval;
    private final Set<String> units;
    private final Map<String, MultiTenantAtLeastOnceProcessor> processors = new HashMap<>();
    
    public AbstractTenantAwareAtLeastOnceMessageType(final MultiTenantAtLeastOnceProcessorFactory processorFactory,
                                                     final long refreshInterval,
                                                     final Set<String> units) {
        if (processorFactory == null) {
            throw new IllegalArgumentException("processorFactory is null");
        }
        
        this.processorFactory = processorFactory;
        this.refreshInterval = refreshInterval;
        this.units = Collections.unmodifiableSet(units);
    }
    
    @Override
    public String getName() {
        return getKey();
    }
    
    public void start() {
        for (final String unit : units) {
            processors.put(unit, DATA_UNIT.exec(unit, () -> processorFactory.create(new DataUnitAtLeastOnceMessageTypeWrapper<>(this), refreshInterval)));
        }
    }
    
    public void stop() {
        for (final AtLeastOnceProcessor processor : processors.values()) {
            processor.stop();
        }
    }
    
    @Override
    public Async<Void> preActivate(final String tenantId) {
        return result();
    }
    
    @Override
    public Async<Void> activate(final String tenantId) {
        final List<Async<Void>> activations = processors.values()
            .stream()
            .map(processor -> processor.registerTenant(tenantId))
            .collect(Collectors.toList());
        return all(activations).map(ok -> null);
    }
    
    @Override
    public Async<Void> deactivate(final String tenantId) {
        final List<Async<Void>> deactivations = processors.values()
            .stream()
            .map(processor -> processor.deregisterTenant(tenantId))
            .collect(Collectors.toList());
        return all(deactivations).map(ok -> null);
    }
    
    @Override
    public Async<Void> postDeactivate(final String tenantId) {
        return result();
    }
    
    protected void pollNow() {
        final MultiTenantAtLeastOnceProcessor processor = processors.get(DATA_UNIT.get());
        if (processor != null) {
            processor.pollNow();
        }
    }
    
}
