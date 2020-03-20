package com.ixaris.commons.multitenancy.lib.object;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.misc.lib.lock.LockUtil;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.TenantLifecycleParticipant;
import com.ixaris.commons.multitenancy.lib.exception.MissingMultiTenantObjectException;

/**
 * Eager implementation of {@link MultiTenantObject}. Why eager? The objects for tenants that this class encapsulates are expected to be eagerly
 * created before the tenant uses them. Attempts at using objects for tenants which have not been created yet will fail with an {@link
 * MissingMultiTenantObjectException}.
 *
 * @author benjie.gatt
 */
public abstract class AbstractEagerMultiTenantObject<T, C> implements MultiTenantObject<T>, TenantLifecycleParticipant {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractEagerMultiTenantObject.class);
    
    // mapping between tenant id and its concrete instance for each available tenant.
    private final Map<String, T> tenantInstances = new HashMap<>();
    
    // used to maintain thread safety when updating tenantInstances
    protected final StampedLock lock = new StampedLock();
    
    private final String name;
    
    /**
     * Create a new instance of an eagerly created multi-tenanted object. Note: before use, each tenant must be added via {@link
     * #addTenant(String, Object)}
     *
     * @param name the name given to this object. Used mostly for logging in case of issues, to be able to identify what the wrapped object is
     */
    public AbstractEagerMultiTenantObject(final String name) {
        this.name = name;
    }
    
    @Override
    public final String getName() {
        return name;
    }
    
    /**
     * Prepares the object for the provided tenant. Must be called before this tenant tries to use the object via {@link #get()}!
     *
     * @param tenantId the tenant the object belongs to
     * @param create an object to support creating the tenant instance, typically configuration like connection strings, etc
     */
    protected final void addTenant(final String tenantId, final C create) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is null");
        }
        
        LockUtil.write(lock, () -> {
            if (!tenantInstances.containsKey(tenantId)) {
                tenantInstances.put(tenantId, create(create, tenantId));
            } else {
                throw new IllegalStateException("Tenant [" + tenantId + "] already has a instance configured");
            }
        });
    }
    
    /**
     * Destroy the object that the tenant holds in this wrapper. Used primarily for cleanup purposes, and when the tenant is removed globally.
     * Should be called for cleanup when a tenant is deactivated. Otherwise, we end up with unused references hogging up memory. The objects also
     * perform their own cleanup when this is called (eg. removal of connections).
     *
     * @param tenantId the tenant to which the object belongs to.
     */
    protected final void removeTenant(final String tenantId) {
        LockUtil.write(lock, () -> {
            final T removed = tenantInstances.remove(tenantId);
            if (removed != null) {
                try {
                    destroy(removed, tenantId);
                    LOG.info("Destroyed tenant object [{}] for tenant [{}]", this.getName(), tenantId);
                } catch (final RuntimeException e) {
                    LOG.warn("Failed to destroy [" + removed + "] for tenant [" + tenantId + "]", e);
                }
            }
        });
    }
    
    @Override
    public final T get() {
        final String tenantId = MultiTenancy.getCurrentTenant();
        
        return LockUtil.read(lock, true, () -> {
            final T instance = tenantInstances.get(tenantId);
            
            // We should always have an object for each tenant
            if (instance == null) {
                throw new MissingMultiTenantObjectException("Missing eagerly created object of type [" + name + "] for tenant [" + tenantId + "]");
            }
            
            return instance;
        });
    }
    
    /**
     * Create an instance of an object for a tenant. Each object can perform instantiated different and should override this method. Does not
     * need to be synchronised as create/destroy are synchronized by this class
     *
     * @param create an object to support creating the tenant instance, typically configuration like connection strings, etc
     * @param tenantId the tenant for which the object is being created
     * @return an instance of the object being wrapped instantiated for a particular tenant
     */
    protected abstract T create(final C create, final String tenantId);
    
    /**
     * Destroy the object, and perform any cleanup required. Does not need to be synchronised as create/destroy are synchronized by this class
     *
     * @param instance the instance being removed
     * @param tenantId the tenant id to which it belongs
     */
    protected abstract void destroy(final T instance, final String tenantId);
    
}
