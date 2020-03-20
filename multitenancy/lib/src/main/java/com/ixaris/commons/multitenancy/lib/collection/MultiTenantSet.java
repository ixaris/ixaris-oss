package com.ixaris.commons.multitenancy.lib.collection;

import java.util.Set;
import java.util.function.Supplier;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.object.LazyMultiTenantObject;

/**
 * See {@link LazyMultiTenantObject}
 *
 * <p>Created by ian.grima on 08/10/2015.
 */
public class MultiTenantSet<E, S extends Set<E>> extends AbstractMultiTenantCollection<E, S> implements Set<E> {
    
    public MultiTenantSet(final MultiTenancy multiTenancy, final Supplier<S> newInstanceSupplier) {
        super(multiTenancy, newInstanceSupplier);
    }
    
}
