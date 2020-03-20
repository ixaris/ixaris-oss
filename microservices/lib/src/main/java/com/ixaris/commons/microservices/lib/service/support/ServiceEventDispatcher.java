package com.ixaris.commons.microservices.lib.service.support;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;

/**
 * A service event dispatcher is responsible for receiving service events from a message source and pass it to the
 * subscriber. It also supports an acknowledgement result to indicate that the event was acknowledged by the service and
 * can request more. The latter depends on the technology being used for events.
 */
public interface ServiceEventDispatcher {
    
    Async<EventAckEnvelope> dispatch(EventEnvelope eventEnvelope);
    
}
