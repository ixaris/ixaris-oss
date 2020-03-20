package com.ixaris.commons.microservices.lib.client;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.misc.lib.function.CallableThrows;

/**
 * Listener interface for service events. For frameworks that support acknowledging the event, ACK should be sent when the future is successfully
 * completed
 *
 * <p>NOTE: at least once semantics are the framework's responsibility. Exactly once semantics are the client's responsibility, typically
 * achieved through idempotent event processing.
 *
 * @param <C> Header Context Type
 * @param <T> Event Type
 * @author brian.vella
 */
@FunctionalInterface
public interface ServiceEventListener<C extends MessageLite, T extends MessageLite> {
    
    Async<Void> onEvent(ServiceEventHeader<C> header, T event);
    
    /**
     * Return a subscriber name or empty string to use the global subscriber name
     */
    default String getName() {
        return "";
    }
    
    /**
     * Listeners handle events in 2 phases: routing and invoking
     *
     * <p>This method defines the routing call, if routing can be done with the information available in the envelope,
     * without knowledge of the payload. Once the event envelope is routed to the correct node, the listener is invoked
     * and an event ack envelope is created, which is then used to fulfill the returned promise. If routing requires
     * information in the payload, then the routing should be done in the {{@link #onEvent(ServiceEventHeader,
     * MessageLite)} method.
     *
     * <p>Delivery of the event envelope to the target node and the event ack envelope back to the original caller is up
     * to the implementation.
     *
     * <p>To invoke the operation, use either {@link ServiceStubEvent#invokeOnListener()} or {@link
     * ServiceStubEvent#getResourceEventObject()}
     *
     * @param event
     * @return
     */
    default Async<EventAckEnvelope> handle(final ServiceStubEvent event) {
        return event.invokeOnListener();
    }
    
    /**
     * Can perform some logic around service event for this listener
     *
     * @param callable
     * @return the result of the callable called with around logic
     */
    default <X, E extends Exception> X aroundAsync(CallableThrows<X, E> callable) throws E {
        return callable.call();
    }
    
}
