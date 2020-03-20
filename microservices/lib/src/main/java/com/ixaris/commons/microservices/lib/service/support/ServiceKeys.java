package com.ixaris.commons.microservices.lib.service.support;

import java.util.Set;

import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;

/**
 * Implement to provide keys for spis. Typicaly from either static configuration for service implementations, e.g. a
 * properties file or an annotation, or some dynamic configuration for gateways, e.g. a database or central
 * configuration
 */
public interface ServiceKeys {
    
    Set<String> get(Class<? extends ServiceSkeleton> serviceSkeletonType, ServiceSkeleton serviceSkeleton);
    
}
