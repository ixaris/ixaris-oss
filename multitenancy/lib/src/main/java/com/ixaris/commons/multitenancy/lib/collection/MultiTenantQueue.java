package com.ixaris.commons.multitenancy.lib.collection;

import java.util.Queue;
import java.util.function.Supplier;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;

public class MultiTenantQueue<E, Q extends Queue<E>> extends AbstractMultiTenantCollection<E, Q> implements Queue<E> {
    
    public MultiTenantQueue(final MultiTenancy multiTenancy, final Supplier<Q> newInstanceSupplier) {
        super(multiTenancy, newInstanceSupplier);
    }
    
    @Override
    public final boolean offer(final E e) {
        return get().offer(e);
    }
    
    @Override
    public final E remove() {
        return get().remove();
    }
    
    @Override
    public final E poll() {
        return get().poll();
    }
    
    @Override
    public final E element() {
        return get().element();
    }
    
    @Override
    public final E peek() {
        return get().peek();
    }
    
}
