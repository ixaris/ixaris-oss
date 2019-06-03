package com.ixaris.commons.collections.lib;

import com.ixaris.commons.misc.lib.object.Wrapper;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * Wraps a list and converts the items from one class to another. This is used to avoid iterating through the list twice
 *
 * @author matthew.croker
 * @param <X> the original list type
 * @param <E> the new list type
 * @param <C> the original list collection type
 */
public abstract class ConvertingListWrapper<X, E, C extends List<X>> extends ConvertingCollectionWrapper<X, E, C>
implements List<E> {
    
    /**
     * Wrap a sublist (ideally using the same class used to wrap the full list)
     *
     * @param wrapped
     * @return
     */
    protected abstract List<E> wrap(List<X> wrapped);
    
    public ConvertingListWrapper(final C wrapped) {
        super(wrapped);
    }
    
    @Override
    public boolean addAll(final int index, final Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public E get(final int index) {
        return convert(wrapped.get(index));
    }
    
    @Override
    public E set(final int index, final E element) {
        return convert(wrapped.set(index, reverseConvert(element)));
    }
    
    @Override
    public void add(final int index, final E element) {
        wrapped.add(index, reverseConvert(element));
    }
    
    @Override
    public E remove(final int index) {
        return convert(wrapped.remove(index));
    }
    
    @Override
    public int indexOf(final Object o) {
        return wrapped.indexOf(reverseConvertObject(o));
    }
    
    @Override
    public int lastIndexOf(final Object o) {
        return wrapped.lastIndexOf(reverseConvertObject(o));
    }
    
    @Override
    public ListIterator<E> listIterator() {
        return new ConvertingListIteratorWrapper(wrapped.listIterator());
    }
    
    @Override
    public ListIterator<E> listIterator(final int index) {
        return new ConvertingListIteratorWrapper(wrapped.listIterator(index));
    }
    
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return wrap(wrapped.subList(fromIndex, toIndex));
    }
    
    public final class ConvertingListIteratorWrapper implements ListIterator<E>, Wrapper<ListIterator<X>> {
        
        private final ListIterator<X> wrapped;
        
        public ConvertingListIteratorWrapper(final ListIterator<X> wrapped) {
            this.wrapped = wrapped;
        }
        
        @Override
        public boolean hasNext() {
            return wrapped.hasNext();
        }
        
        @Override
        public E next() {
            return convert(wrapped.next());
        }
        
        @Override
        public void remove() {
            wrapped.remove();
        }
        
        @Override
        public boolean hasPrevious() {
            return wrapped.hasPrevious();
        }
        
        @Override
        public E previous() {
            return convert(wrapped.previous());
        }
        
        @Override
        public int nextIndex() {
            return wrapped.nextIndex();
        }
        
        @Override
        public int previousIndex() {
            return wrapped.previousIndex();
        }
        
        @Override
        public void set(final E e) {
            wrapped.set(reverseConvert(e));
        }
        
        @Override
        public void add(final E e) {
            wrapped.add(reverseConvert(e));
        }
        
        @Override
        public ListIterator<X> unwrap() {
            return wrapped;
        }
        
    }
    
}
