package com.ixaris.commons.microservices.lib.client;

import java.util.Optional;

import com.ixaris.commons.microservices.lib.common.annotations.ServiceApi;

/**
 * Service stub interface. Can be used by spring to detect that a proxy should be created
 *
 * @author brian.vella
 */
public interface ServiceStub {
    
    static String extractServiceName(final Class<? extends ServiceStub> serviceStubType) {
        return Optional.ofNullable(serviceStubType.getAnnotation(ServiceApi.class))
            .map(ServiceApi::value)
            .orElseThrow(() -> new IllegalStateException("Expecting @ServiceApi annotation on " + serviceStubType));
    }
    
}
