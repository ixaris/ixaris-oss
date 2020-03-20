package com.ixaris.commons.microservices.lib.client.support;

import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.exception.ServerUnavailableException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.ServiceSkeletonOperation;

final class MultiplexingServiceOperationDispatcher implements ServiceOperationDispatcher {
    
    private final ServiceOperationDispatcher[] dispatchers;
    
    MultiplexingServiceOperationDispatcher(final ServiceOperationDispatcher... dispatchers) {
        this.dispatchers = dispatchers;
    }
    
    @Override
    public Async<ResponseEnvelope> dispatch(final RequestEnvelope requestEnvelope) {
        if (dispatchers.length > 0) {
            for (final ServiceOperationDispatcher dispatcher : dispatchers) {
                if (dispatcher.isKeyAvailable(requestEnvelope.getServiceKey())) {
                    return dispatcher.dispatch(requestEnvelope);
                }
            }
            // service unavailable default to the last dispatcher
            return dispatchers[dispatchers.length - 1].dispatch(requestEnvelope);
        } else {
            return result(ServiceSkeletonOperation.wrapError(requestEnvelope, new ServerUnavailableException()));
        }
    }
    
    @Override
    public boolean isKeyAvailable(final String key) {
        for (final ServiceOperationDispatcher dispatcher : dispatchers) {
            if (dispatcher.isKeyAvailable(key)) {
                return true;
            }
        }
        return false;
    }
    
}
