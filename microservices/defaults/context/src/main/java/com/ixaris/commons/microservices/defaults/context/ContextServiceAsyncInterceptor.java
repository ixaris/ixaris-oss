package com.ixaris.commons.microservices.defaults.context;

import static com.ixaris.commons.microservices.defaults.context.AsyncLocals.HEADER;

import org.springframework.stereotype.Component;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.lib.common.ServiceHeader;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.misc.lib.function.CallableThrows;

@Component
public final class ContextServiceAsyncInterceptor implements ServiceAsyncInterceptor {
    
    @SuppressWarnings("unchecked")
    @Override
    public <T, E extends Exception> T aroundAsync(final ServiceHeader<?> header, final CallableThrows<T, E> callable) throws E {
        if (header.getContext() instanceof Context) {
            return HEADER.exec((ServiceHeader<Context>) header, callable);
        } else {
            return callable.call();
        }
    }
    
}
