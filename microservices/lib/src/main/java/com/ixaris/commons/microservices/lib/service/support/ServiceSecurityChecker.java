package com.ixaris.commons.microservices.lib.service.support;

import java.util.List;

import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;

/**
 * Given a context instance and a security descriptor, check whether the required security level is satisfied
 */
@FunctionalInterface
public interface ServiceSecurityChecker {
    
    /**
     * @param header
     * @param security
     * @param tags
     * @return true if the required security level is satisfied, false otherwise
     */
    boolean check(ServiceOperationHeader<?> header, String security, List<String> tags);
    
}
