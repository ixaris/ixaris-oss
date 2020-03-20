package com.ixaris.commons.multitenancy.lib.async;

import com.ixaris.commons.clustering.lib.idempotency.AtLeastOnceMessageType;
import com.ixaris.commons.clustering.lib.idempotency.AtLeastOnceProcessorFactory;

@FunctionalInterface
public interface MultiTenantAtLeastOnceProcessorFactory extends AtLeastOnceProcessorFactory {
    
    @Override
    MultiTenantAtLeastOnceProcessor create(AtLeastOnceMessageType<?> messageType, long refreshInterval);
    
}
