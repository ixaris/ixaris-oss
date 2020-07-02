package com.ixaris.commons.persistence.microservices;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;

/**
 * An event publisher with at-least-once delivery semantics.
 *
 * <p>Delivery of events to a message broker can fail. This ServiceEventPublisher wrapper makes sure that events are
 * resent until the broker acknowledges the delivery. When used with a persistent store, events are redelivered even
 * after the microservice is restarted. To work properly, events must be stored if and only if the rest of the business
 * logic succeeds (e.g. within the same transaction when the microservice is backed by an SQL database).
 *
 * <p>This publisher NEVER delivers events directly from publish/publishEnvelope methods. New events are regularly
 * polled from the store and eventually delivered.
 */
public interface AtLeastOnceServiceEventPublisher<C extends MessageLite, E extends MessageLite, R> {
    
    /**
     * Publish the event with at-most-once publish guarantees. If the event is published/persisted, the method executes
     * normally, otherwise an exception is thrown
     *
     * @param header The service header that describes the event
     * @param event The event message with all the details to be published
     */
    void publish(final ServiceEventHeader<C> header, final E event);
    
}
