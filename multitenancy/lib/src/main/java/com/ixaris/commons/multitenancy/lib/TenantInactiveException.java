package com.ixaris.commons.multitenancy.lib;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class TenantInactiveException extends Exception {
    
    private static String formatErrors(final Set<TenantLifecycleParticipant> errors) {
        return Optional.ofNullable(errors)
            .map(e -> e.isEmpty() ? null : e)
            .map(e -> e.stream().map(TenantLifecycleParticipant::getName).collect(Collectors.joining(", ")))
            .map(e -> e + " in error")
            .orElse("No errors");
    }
    
    public TenantInactiveException(final String tenantId) {
        super(String.format("Tenant [%s] is inactive (UNKNOWN)", tenantId));
    }
    
    public TenantInactiveException(final String tenantId, final String state, final Set<TenantLifecycleParticipant> errors) {
        super(String.format("Tenant [%s] is inactive (%s) %s", tenantId, state, formatErrors(errors)));
    }
}
