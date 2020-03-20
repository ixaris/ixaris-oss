package com.ixaris.commons.multitenancy.lib.object;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.HashMap;
import java.util.Map;

import com.ixaris.commons.async.lib.Async;

public final class TestEagerMultiTenantObject extends AbstractEagerMultiTenantObject<DataHolder, String> {
    
    private final Map<String, String> tenantProps = new HashMap<>();
    private final Map<String, DataHolder> destroyed = new HashMap<>();
    
    public TestEagerMultiTenantObject(final String name) {
        super(name);
    }
    
    @Override
    protected DataHolder create(final String prop, final String tenantId) {
        return new DataHolder(prop);
    }
    
    @Override
    protected void destroy(final DataHolder instance, final String tenantId) {
        destroyed.put(tenantId, instance);
    }
    
    @Override
    public Async<Void> preActivate(final String tenantId) {
        return result();
    }
    
    @Override
    public Async<Void> activate(final String tenantId) {
        addTenant(tenantId, tenantProps.get(tenantId));
        return result();
    }
    
    @Override
    public Async<Void> deactivate(final String tenantId) {
        removeTenant(tenantId);
        return result();
    }
    
    @Override
    public Async<Void> postDeactivate(final String tenantId) {
        return result();
    }
    
    public DataHolder getDestroyed(String tenantId) {
        return destroyed.get(tenantId);
    }
    
    public void setTenantProp(final String tenantId, final String prop) {
        tenantProps.put(tenantId, prop);
    }
    
}
