package com.ixaris.commons.microservices.lib.service.support;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;

public interface ServiceOperationProcessor {
    
    Async<ResponseEnvelope> process(RequestEnvelope requestEnvelope);
    
}
