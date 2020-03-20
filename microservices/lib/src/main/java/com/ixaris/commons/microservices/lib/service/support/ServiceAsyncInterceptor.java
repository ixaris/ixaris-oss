package com.ixaris.commons.microservices.lib.service.support;

import com.ixaris.commons.microservices.lib.common.ServiceHeader;
import com.ixaris.commons.misc.lib.function.CallableThrows;

@FunctionalInterface
public interface ServiceAsyncInterceptor {
    
    ServiceAsyncInterceptor PASSTHROUGH = new ServiceAsyncInterceptor() {
        
        @Override
        public <T, E extends Exception> T aroundAsync(final ServiceHeader<?> header, final CallableThrows<T, E> callable) throws E {
            return callable.call();
        }
        
    };
    
    /**
     * Can perform some logic around (potentially) async logic. This is used by infrastructure code around invocations
     * of operations on resource implementation or event listener.
     *
     * <p>Typically, async locals are set here, e.g. for subject
     *
     * @return the (potentially intercepted) result of the callable
     */
    <T, E extends Exception> T aroundAsync(ServiceHeader<?> header, CallableThrows<T, E> callable) throws E;
    
}
