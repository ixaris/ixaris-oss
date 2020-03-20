package com.ixaris.commons.microservices.lib.service;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;

public interface ServiceResponseWrapper<R, C> {
    
    ResponseEnvelope result(R result);
    
    ResponseEnvelope conflict(C conflict);
    
    ResponseEnvelope error(Throwable t);
    
}
