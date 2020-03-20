package com.ixaris.commons.multitenancy.lib.collection;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.object.LazyMultiTenantObject;

/**
 * See {@link LazyMultiTenantObject}
 *
 * @author <a href="mailto:matthias.portelli@ixaris.com">matthias.portelli</a>
 */
public class MultiTenantList<E, L extends List<E>> extends AbstractMultiTenantCollection<E, L> implements List<E> {
    
    public MultiTenantList(final MultiTenancy multiTenancy, final Supplier<L> newInstanceSupplier) {
        super(multiTenancy, newInstanceSupplier);
    }
    
    @Override
    public void replaceAll(final UnaryOperator<E> operator) {
        get().replaceAll(operator);
    }
    
    @Override
    public List<E> subList(final int fromIndex, final int toIndex) {
        return get().subList(fromIndex, toIndex);
    }
    
    @Override
    public ListIterator<E> listIterator(final int index) {
        return get().listIterator(index);
    }
    
    @Override
    public ListIterator<E> listIterator() {
        return get().listIterator();
    }
    
    @Override
    public int lastIndexOf(final Object o) {
        return get().lastIndexOf(o);
    }
    
    @Override
    public int indexOf(final Object o) {
        return get().indexOf(o);
    }
    
    @Override
    public E remove(final int index) {
        return get().remove(index);
    }
    
    @Override
    public void add(final int index, final E element) {
        get().add(index, element);
    }
    
    @Override
    public E set(final int index, final E element) {
        return get().set(index, element);
    }
    
    @Override
    public E get(final int index) {
        return get().get(index);
    }
    
    @Override
    public boolean addAll(final int index, final Collection<? extends E> c) {
        return get().addAll(index, c);
    }
    
    @Override
    public void sort(final Comparator<? super E> c) {
        get().sort(c);
    }
    
}
