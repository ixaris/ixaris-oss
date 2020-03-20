package com.ixaris.commons.multitenancy.lib.collection;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.object.LazyMultiTenantObject;

/**
 * See {@link LazyMultiTenantObject}
 *
 * <p>Created by ian.grima on 09/10/2015.
 */
public class MultiTenantBlockingQueue<E, Q extends BlockingQueue<E>> extends MultiTenantQueue<E, Q> implements BlockingQueue<E> {
    
    public MultiTenantBlockingQueue(final MultiTenancy multiTenancy, final Supplier<Q> newInstanceSupplier) {
        super(multiTenancy, newInstanceSupplier);
    }
    
    @Override
    public void put(final E e) throws InterruptedException {
        get().put(e);
    }
    
    @Override
    public boolean offer(final E e, final long timeout, final TimeUnit unit) throws InterruptedException {
        return get().offer(e, timeout, unit);
    }
    
    @Override
    public E take() throws InterruptedException {
        return get().take();
    }
    
    @Override
    public E poll(final long timeout, final TimeUnit unit) throws InterruptedException {
        return get().poll(timeout, unit);
    }
    
    @Override
    public int remainingCapacity() {
        return get().remainingCapacity();
    }
    
    @Override
    public int drainTo(final Collection<? super E> c) {
        return get().drainTo(c);
    }
    
    @Override
    public int drainTo(final Collection<? super E> c, int maxElements) {
        return get().drainTo(c, maxElements);
    }
    
}
