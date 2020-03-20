package com.ixaris.commons.microservices.defaults.context;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Subject;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;

@Component
public class ContextServiceSecurityChecker implements ServiceSecurityChecker {
    
    public static final String UNSECURED_SECURITY_TAG = "UNSECURED";
    
    @Override
    public boolean check(final ServiceOperationHeader<?> header, final String security, final List<String> tags) {
        if (header.getContext() instanceof Context) {
            if (UNSECURED_SECURITY_TAG.equals(security)) {
                return true;
            }
            
            final Subject subject = ((Context) header.getContext()).getSubject();
            if (!header.getTenantId().equals(subject.getTenantId())) {
                return false;
            }
            return isValidSubject(subject);
        } else {
            return false;
        }
    }
    
    private boolean isValidSubject(final Subject subject) {
        return subject.getSessionId() > 0L;
    }
    
}
