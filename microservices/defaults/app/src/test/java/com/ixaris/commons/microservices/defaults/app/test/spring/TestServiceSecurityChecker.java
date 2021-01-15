package com.ixaris.commons.microservices.defaults.app.test.spring;

import java.util.List;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;

/**
 * @author <a href="mailto:brian.vella@ixaris.com">brian.vella</a>
 */
public class TestServiceSecurityChecker implements ServiceSecurityChecker {
    
    private static final String UNSECURED_SECURITY_TAG = "UNSECURED";
    
    @Override
    public boolean check(final ServiceOperationHeader<?> header, final String security, final List<String> tags) {
        if (header.getContext() instanceof Context) {
            return UNSECURED_SECURITY_TAG.equals(security) || containsValidSubject((Context) header.getContext());
        } else {
            return false;
        }
    }
    
    private static boolean containsValidSubject(final Context context) {
        return context.hasSubject() && context.getSubject().getSessionId() > 0L;
    }
}
