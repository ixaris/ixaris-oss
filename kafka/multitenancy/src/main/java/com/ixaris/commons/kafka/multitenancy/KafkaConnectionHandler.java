package com.ixaris.commons.kafka.multitenancy;

import java.util.Set;

import com.ixaris.commons.async.lib.Async;

/**
 * @author <a href="mailto:Armand.Sciberras@ixaris.com">Armand.Sciberras</a>
 */
public interface KafkaConnectionHandler {
    
    void register(Set<String> topics);
    
    Async<Void> publish(String qualifiedEventName, long partitionId, byte[] message);
    
    void subscribe(String subscriberName, String eventName, KafkaMessageHandler messageHandler);
    
    void unsubscribe(String subscriberName, String eventName);
    
}
