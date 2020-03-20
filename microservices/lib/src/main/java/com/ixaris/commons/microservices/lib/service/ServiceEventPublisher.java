package com.ixaris.commons.microservices.lib.service;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.EventAck;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;

/**
 * The {@link ServiceEventPublisher} provides functionality related to publishing of events. A promise is returned to indicate whether the
 * publishing was successful
 *
 * @author aldrin.seychell
 * @param <C> The service Context protobuf type
 * @param <E> The event protobuf type
 */
@SuppressWarnings("squid:S2326")
public interface ServiceEventPublisher<C extends MessageLite, E extends MessageLite, R> {
    
    Class<? extends ServiceSkeleton> getSkeletonType();
    
    ServicePathHolder getPath();
    
    /**
     * Publish the event with at-most-once publish guarantees. The event is published and a promise with the event acknowledgement is returned.
     *
     * @param header The service header that describes the event
     * @param event The event message with all the details to be published
     * @return Promise representing whether the event was published and acknowledged or not.
     */
    Async<EventAck> publish(ServiceEventHeader<C> header, E event);
    
}
