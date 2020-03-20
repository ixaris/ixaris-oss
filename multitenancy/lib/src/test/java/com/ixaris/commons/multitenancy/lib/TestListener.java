package com.ixaris.commons.multitenancy.lib;

import java.util.LinkedList;
import java.util.List;

public class TestListener implements TenantLifecycleListener {
    
    private final List<String> activated = new LinkedList<>();
    private final List<String> deactivated = new LinkedList<>();
    
    @Override
    public void onTenantActive(final String tenantId) {
        activated.add(tenantId);
    }
    
    @Override
    public void onTenantInactive(final String tenantId) {
        deactivated.add(tenantId);
    }
    
    public List<String> getActivated() {
        return activated;
    }
    
    public List<String> getDeactivated() {
        return deactivated;
    }
    
}
