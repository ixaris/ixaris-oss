package com.ixaris.commons.multitenancy.lib.object;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import com.ixaris.commons.misc.lib.lock.LockUtil;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.TenantLifecycleListener;

/**
 * A wrapper that tracks an instance for each active tenant. The objects that this class tracks can be lazily
 * instantiated. As such, getting an object which does not exist yet for a new tenant will cause the object to be
 * created dynamically, in contrast to {@link AbstractEagerMultiTenantObject}.
 *
 * @author benjie.gatt
 * @param <T> the type of the wrapped instance
 */
public class LazyMultiTenantObject<T> implements MultiTenantObject<T> {
    
    private final MultiTenancy multiTenancy;
    private final Supplier<T> newInstanceSupplier;
    
    // used to maintain thread safety when updating {@code tenantInstances}
    private final StampedLock lock = new StampedLock();
    
    // Holds an Object instance for each tenant
    private final Map<String, T> tenantInstances = new HashMap<>();
    
    public LazyMultiTenantObject(final MultiTenancy multiTenancy, final Supplier<T> newInstanceSupplier) {
        this.multiTenancy = multiTenancy;
        this.newInstanceSupplier = newInstanceSupplier;
    }
    
    @Override
    public final T get() {
        final String tenantId = MultiTenancy.getCurrentTenant();
        
        return LockUtil.readMaybeWrite(lock,
            true,
            () -> tenantInstances.get(tenantId),
            Objects::nonNull,
            () -> {
                if (tenantInstances.isEmpty()) {
                    // register a listener that removes instances for inactive tenants
                    multiTenancy.addTenantLifecycleListener(new TenantLifecycleListener() {
                        
                        @Override
                        public void onTenantActive(final String tenantId) {
                            // do nothing on tenant active as these instances are created lazily
                        }
                        
                        @Override
                        public void onTenantInactive(final String tenantId) {
                            LockUtil.write(lock, () -> {
                                tenantInstances.remove(tenantId);
                                if (tenantInstances.isEmpty()) {
                                    multiTenancy.removeTenantLifecycleListener(this);
                                }
                            });
                        }
                        
                    });
                }
                return tenantInstances.computeIfAbsent(tenantId, k -> newInstanceSupplier.get());
            });
    }
    
}
