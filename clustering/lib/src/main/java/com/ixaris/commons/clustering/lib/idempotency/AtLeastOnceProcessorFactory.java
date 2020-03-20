package com.ixaris.commons.clustering.lib.idempotency;

@FunctionalInterface
public interface AtLeastOnceProcessorFactory {
    
    AtLeastOnceProcessor create(AtLeastOnceMessageType<?> messageType, long refreshInterval);
    
}
