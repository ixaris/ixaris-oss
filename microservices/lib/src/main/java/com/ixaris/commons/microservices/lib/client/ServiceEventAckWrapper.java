package com.ixaris.commons.microservices.lib.client;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;

public interface ServiceEventAckWrapper {
    
    EventAckEnvelope success();
    
    EventAckEnvelope error(Throwable t);
    
}
