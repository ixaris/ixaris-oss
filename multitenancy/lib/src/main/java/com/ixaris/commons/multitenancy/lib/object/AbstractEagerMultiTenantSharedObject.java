package com.ixaris.commons.multitenancy.lib.object;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object allows tenants to share the same object, e.g. connection pool to the same database. This tracks usage count and destroys the
 * object when the count goes to 0.
 *
 * @param <T>
 * @param <U>
 * @param <C>
 */
public abstract class AbstractEagerMultiTenantSharedObject<T, U, C> extends AbstractEagerMultiTenantObject<T, C> {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractEagerMultiTenantSharedObject.class);
    
    private static final class InstanceAndUsage<T> {
        
        private final T instance;
        private int usageCount = 0;
        
        InstanceAndUsage(final T instance) {
            this.instance = instance;
        }
        
    }
    
    private final Map<String, InstanceAndUsage<U>> instances = new HashMap<>();
    private final Map<String, String> tenantHashes = new HashMap<>();
    
    public AbstractEagerMultiTenantSharedObject(final String name) {
        super(name);
    }
    
    protected abstract String computeHash(C create);
    
    protected abstract U createShared(C create);
    
    protected abstract T wrap(U instance, String tenantId);
    
    protected abstract void destroyShared(T instance);
    
    @Override
    protected final T create(final C create, final String tenantId) {
        final String hash = computeHash(create);
        final InstanceAndUsage<U> instanceAndUsage = instances.computeIfAbsent(hash, k -> {
            final U instance = createShared(create);
            return new InstanceAndUsage<>(instance);
        });
        
        instanceAndUsage.usageCount++;
        if (instanceAndUsage.usageCount > 1) {
            LOG.info("Reusing instance for tenant [{}]", tenantId);
        }
        
        tenantHashes.put(tenantId, hash);
        return wrap(instanceAndUsage.instance, tenantId);
    }
    
    @Override
    protected final void destroy(final T instance, final String tenantId) {
        final String hash = tenantHashes.remove(tenantId);
        if (hash != null) {
            final InstanceAndUsage instanceAndUsage = instances.get(hash);
            if (instanceAndUsage.usageCount > 1) {
                instanceAndUsage.usageCount--;
            } else {
                instances.remove(hash);
                destroyShared(instance);
            }
        }
    }
    
}
