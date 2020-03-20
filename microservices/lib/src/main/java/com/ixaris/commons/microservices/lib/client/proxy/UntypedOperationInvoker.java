package com.ixaris.commons.microservices.lib.client.proxy;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;

public class UntypedOperationInvoker {
    
    @FunctionalInterface
    public interface KeyAvailableCondition {
        
        boolean isKeyAvailable(String key);
        
    }
    
    private final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain;
    private final KeyAvailableCondition keyAvailableCondition;
    
    public UntypedOperationInvoker(final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain,
                                   final KeyAvailableCondition keyAvailableCondition) {
        if (filterChain == null) {
            throw new IllegalArgumentException("filterChain is null");
        }
        if (keyAvailableCondition == null) {
            throw new IllegalArgumentException("keyAvailableCondition is null");
        }
        
        this.filterChain = filterChain;
        this.keyAvailableCondition = keyAvailableCondition;
    }
    
    public final Async<ResponseEnvelope> invoke(final RequestEnvelope requestEnvelope) {
        return filterChain.next(requestEnvelope);
    }
    
    public boolean isKeyAvailable(final String key) {
        return keyAvailableCondition.isKeyAvailable(key);
    }
    
}
