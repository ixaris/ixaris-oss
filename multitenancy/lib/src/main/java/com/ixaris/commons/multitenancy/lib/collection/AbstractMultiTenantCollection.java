package com.ixaris.commons.multitenancy.lib.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.object.LazyMultiTenantObject;

/**
 * See {@link LazyMultiTenantObject}
 *
 * <p>Created by ian.grima on 09/10/2015.
 */
public abstract class AbstractMultiTenantCollection<E, C extends Collection<E>> extends LazyMultiTenantObject<C> implements Collection<E> {
    
    public AbstractMultiTenantCollection(final MultiTenancy multiTenancy, final Supplier<C> newInstanceSupplier) {
        super(multiTenancy, newInstanceSupplier);
    }
    
    @Override
    public final int size() {
        return get().size();
    }
    
    @Override
    public final boolean isEmpty() {
        return get().isEmpty();
    }
    
    @Override
    public final boolean contains(final Object o) {
        return get().contains(o);
    }
    
    @Override
    public final Iterator<E> iterator() {
        return get().iterator();
    }
    
    @Override
    public final Object[] toArray() {
        return get().toArray();
    }
    
    @Override
    public final <T> T[] toArray(final T... a) {
        return get().toArray(a);
    }
    
    @Override
    public final boolean add(final E e) {
        return get().add(e);
    }
    
    @Override
    public final boolean remove(final Object o) {
        return get().remove(o);
    }
    
    @Override
    public final boolean containsAll(final Collection<?> c) {
        return get().containsAll(c);
    }
    
    @Override
    public final boolean addAll(final Collection<? extends E> c) {
        return get().addAll(c);
    }
    
    @Override
    public final boolean retainAll(final Collection<?> c) {
        return get().retainAll(c);
    }
    
    @Override
    public final boolean removeAll(final Collection<?> c) {
        return get().retainAll(c);
    }
    
    @Override
    public final void clear() {
        get().clear();
    }
    
    @Override
    public final boolean removeIf(final Predicate<? super E> filter) {
        return get().removeIf(filter);
    }
    
    @Override
    public final Spliterator<E> spliterator() {
        return get().spliterator();
    }
    
    @Override
    public final Stream<E> stream() {
        return get().stream();
    }
    
    @Override
    public final Stream<E> parallelStream() {
        return get().parallelStream();
    }
    
    @Override
    public final void forEach(final Consumer<? super E> action) {
        get().forEach(action);
    }
    
}
