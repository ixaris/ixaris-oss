package com.ixaris.commons.microservices.lib.common;

import com.ixaris.commons.async.reactive.RequestStrategy;
import com.ixaris.commons.async.reactive.UnboundedRequestStrategy;
import com.ixaris.commons.misc.lib.function.CallableThrows;

@FunctionalInterface
public interface ServiceHandlerStrategy {
    
    ServiceHandlerStrategy PASSTHROUGH = new ServiceHandlerStrategy() {
        
        @Override
        public <T, E extends Exception> T aroundAsync(final CallableThrows<T, E> callable) throws E {
            return callable.call();
        }
        
    };
    
    /**
     * @return the request strategy for this service
     */
    default RequestStrategy getRequestStrategy() {
        return UnboundedRequestStrategy.getInstance();
    }
    
    /**
     * Can perform some logic around (potentially) service operation and event invocation. This is typically used to set
     * data source unit used.
     *
     * <p>Typically, async locals are set here, e.g. for datasources.
     *
     * @param callable
     * @return the result of the supplier
     */
    <T, E extends Exception> T aroundAsync(CallableThrows<T, E> callable) throws E;
    
}
