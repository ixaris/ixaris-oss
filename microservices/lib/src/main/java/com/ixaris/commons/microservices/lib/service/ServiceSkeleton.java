package com.ixaris.commons.microservices.lib.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceRootResource;
import com.ixaris.commons.microservices.lib.common.annotations.ServiceApi;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.misc.lib.function.CallableThrows;

public interface ServiceSkeleton {
    
    static String extractServiceName(final Class<? extends ServiceSkeleton> serviceSkeletonType) {
        return Optional.ofNullable(serviceSkeletonType.getAnnotation(ServiceApi.class))
            .map(ServiceApi::value)
            .orElseThrow(() -> new IllegalStateException("Expecting @ServiceApi annotation on " + serviceSkeletonType));
    }
    
    @SuppressWarnings("unchecked")
    static Set<Class<? extends ServiceSkeleton>> determineServiceSkeletonTypes(final ServiceSkeleton serviceImpl) {
        return determineServiceSkeletonTypes(serviceImpl.getClass());
    }
    
    /**
     * Looks for every implemented interface that is not itself {@link ServiceSkeleton} but extends it directly, as per
     * scsl code generation. This means that intermediate interfaces between {@link ServiceSkeleton} and the
     * implementing class are not considered.
     *
     * <p>This method assumes that a service implementation can implements more than one service interface. This is
     * slightly convoluted but not impossible.
     *
     * @param serviceImplType the type for which to extract the set of service skeleton interfaces, typically the
     *     implementing class
     * @return the set of ServiceSkeleton interfaces implemented.
     */
    @SuppressWarnings("unchecked")
    static Set<Class<? extends ServiceSkeleton>> determineServiceSkeletonTypes(final Class<? extends ServiceSkeleton> serviceImplType) {
        
        if ((serviceImplType == null) || serviceImplType.equals(ServiceSkeleton.class) || serviceImplType.equals(ServiceProviderSkeleton.class)) {
            return Collections.emptySet();
        }
        
        final Set<Class<? extends ServiceSkeleton>> set = new HashSet<>();
        
        if (serviceImplType.isInterface()) {
            for (final Class<?> i : serviceImplType.getInterfaces()) {
                if (i.equals(ServiceSkeleton.class) || i.equals(ServiceProviderSkeleton.class)) {
                    // we add only interfaces that directly extend ServiceSkeleton
                    set.add(serviceImplType);
                } else if (ServiceSkeleton.class.isAssignableFrom(i)) {
                    set.addAll(determineServiceSkeletonTypes((Class<? extends ServiceSkeleton>) i));
                }
            }
        } else {
            for (final Class<?> i : serviceImplType.getInterfaces()) {
                if (ServiceSkeleton.class.isAssignableFrom(i)) {
                    set.addAll(determineServiceSkeletonTypes((Class<? extends ServiceSkeleton>) i));
                }
            }
        }
        
        final Class<?> superclass = serviceImplType.getSuperclass();
        if ((superclass != null) && ServiceSkeleton.class.isAssignableFrom(superclass)) {
            set.addAll(determineServiceSkeletonTypes((Class<? extends ServiceSkeleton>) superclass));
        }
        
        return set;
    }
    
    ServiceRootResource<?> resource();
    
    /**
     * Service implementations handle operations in 2 phases: routing and invoking
     *
     * <p>This method defines the routing call. Routing should be done with the information available in the envelope
     * (typically on the resource path and parameters), without knowledge of the payload. Once the request envelope is
     * routed to the correct node, the operation is invoked and a response envelope is created, which is then used to
     * fulfill the returned promise.
     *
     * <p>Delivery of the request envelope to the target node and the response envelope back to the original caller is
     * up to the implementation.
     *
     * <p>To invoke the operation, use either {@link ServiceSkeletonOperation#invokeOnResourceProxy()} or {@link
     * ServiceSkeletonOperation#getResourceOperationObject()}
     *
     * @param operation
     * @return
     */
    default Async<ResponseEnvelope> handle(final ServiceSkeletonOperation operation) {
        return operation.invokeOnResourceProxy();
    }
    
    /**
     * Can perform some logic around service operations for this skeleton
     *
     * @param callable
     * @return the result of the callable called with around logic
     */
    default <T, E extends Exception> T aroundAsync(CallableThrows<T, E> callable) throws E {
        return callable.call();
    }
    
}
