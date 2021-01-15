package com.ixaris.commons.kafka.multitenancy;

import com.ixaris.commons.async.lib.Async;

/**
 * @author <a href="mailto:Armand.Sciberras@ixaris.com">Armand.Sciberras</a>
 */
@FunctionalInterface
public interface KafkaMessageHandler {
    
    Async<Void> handle(String partitionKey, byte[] message);
    
}
