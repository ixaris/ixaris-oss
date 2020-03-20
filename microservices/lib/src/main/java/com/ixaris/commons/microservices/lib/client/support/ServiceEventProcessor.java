package com.ixaris.commons.microservices.lib.client.support;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;

public interface ServiceEventProcessor {
    
    Async<EventAckEnvelope> process(EventEnvelope eventEnvelope);
    
}
