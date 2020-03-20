package com.ixaris.commons.multitenancy.lib.object;

/**
 * A wrapper object intended to hide the complexity of multitenancy when using datasources. In general, {@link MultiTenantObject}s act exactly
 * like the wrapped object, with the difference that the wrapper stores a different copy for every tenant.
 *
 * <p>A typical example would be something like a list. There are two approaches - either every tenant would have a list of its own, and all
 * objects would be required to handle picking the right list in their own logic. Alternatively, the list can implement this interface and become
 * a {@link MultiTenantObject}, which has two standard implementations - {@link AbstractEagerMultiTenantObject} and {@link
 * AbstractLazyMultiTenantObject}.
 *
 * <p>The implementations encapsulate the logic of picking the right object for the current tenant. A side effect of this is that all actions on
 * these objects require an active tenant.
 *
 * @author benjie.gatt
 */
public interface MultiTenantObject<T> {
    
    /**
     * @return the object that the active tenant has access to
     */
    T get();
}
