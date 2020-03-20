package com.ixaris.commons.microservices.defaults.test;

import java.util.Objects;

import org.mockito.ArgumentMatcher;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;

/**
 * @author <a href="mailto:bernice.zerafa@ixaris.com">bernice.zerafa</a>
 */
public final class TenantServiceOperationHeaderArgMatcher implements ArgumentMatcher<ServiceOperationHeader<Context>> {
    
    private final String expectedTenantId;
    
    public TenantServiceOperationHeaderArgMatcher(final String expectedTenantId) {
        this.expectedTenantId = expectedTenantId;
    }
    
    @Override
    public boolean matches(final ServiceOperationHeader<Context> argument) {
        return Objects.equals(expectedTenantId, argument.getTenantId());
    }
    
    @Override
    public String toString() {
        return "Expecting operation header with tenant {" + "expectedTenantId='" + expectedTenantId + '\'' + '}';
    }
}
