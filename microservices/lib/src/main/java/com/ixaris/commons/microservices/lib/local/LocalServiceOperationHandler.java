package com.ixaris.commons.microservices.lib.local;

import static com.ixaris.commons.microservices.lib.service.support.ServiceLoggingFilterFactory.OPERATION_CHANNEL;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;

/**
 * Operation handler that processes operation requests and send responses back.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class LocalServiceOperationHandler {
    
    private final String name;
    private final String key;
    private final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain;
    
    LocalServiceOperationHandler(final String name,
                                 final String key,
                                 final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain) {
        this.name = name;
        this.key = key;
        this.filterChain = filterChain;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + name + "/" + key;
    }
    
    Async<ResponseEnvelope> handle(final RequestEnvelope requestEnvelope) {
        return OPERATION_CHANNEL.exec("LOCAL", () -> filterChain.next(requestEnvelope));
    }
    
}
