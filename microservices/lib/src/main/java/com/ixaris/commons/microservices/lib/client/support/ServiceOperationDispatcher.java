package com.ixaris.commons.microservices.lib.client.support;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;

/**
 * A service operation dispatcher is responsible for sending (dispatching) requests to a target service and expects a
 * response envelope to be sent back from the target service.
 */
public interface ServiceOperationDispatcher {
    
    Async<ResponseEnvelope> dispatch(RequestEnvelope requestEnvelope);
    
    boolean isKeyAvailable(String key);
    
}
