package com.ixaris.commons.microservices.defaults.context;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Subject;

public class SystemSubject {
    
    public static final long SYSTEM_ID = 1L;
    public static final String SYSTEM_IDENTITY_TYPE = "system";
    public static final String SYSTEM_CREDENTIAL_TYPE = "system";
    
    public static Subject get(final String tenantId) {
        return Subject.newBuilder()
            .setProgrammeId(SYSTEM_ID)
            .setApplicationId(SYSTEM_ID)
            .setIdentityId(SYSTEM_ID)
            .setIdentityType(SYSTEM_IDENTITY_TYPE)
            .setCredentialId(SYSTEM_ID)
            .setCredentialType(SYSTEM_CREDENTIAL_TYPE)
            .setSessionId(SYSTEM_ID)
            .setTenantId(tenantId)
            .setIsSuperUser(true)
            .build();
    }
    
    public static Subject get() {
        return get(TENANT.get());
    }
    
    private SystemSubject() {}
    
}
