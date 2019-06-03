package com.ixaris.commons.collections.lib;

import com.ixaris.commons.misc.lib.object.Wrapper;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Wraps a collection and converts the items from one class to another. This is used to avoid iterating through the
 * collection twice
 *
 * @author matthew.croker
 * @param <X> the original item type
 * @param <E> the new item type
 * @param <C> the original collection type
 */
public abstract class ConvertingCollectionWrapper<X, E, C extends Collection<X>> extends AbstractCollection<E>
implements Collection<E>, Wrapper<C> {
    
    protected final C wrapped;
    
    /**
     * Convert from the original type to the new type
     *
     * @param item The item
     * @return the converted item
     */
    protected abstract E convert(X item);
    
    /**
     * Reverse convert from the new type to the original type. For a read only conversion, an
     * UnsupportedOperationException can be thrown
     *
     * @param item The item
     * @return the converted item
     */
    protected abstract X reverseConvert(E item);
    
    /**
     * @param item The item
     * @return true if the item is of the original collection Type
     */
    protected abstract boolean isOriginalType(Object item);
    
    public ConvertingCollectionWrapper(final C wrapped) {
        this.wrapped = wrapped;
    }
    
    @SuppressWarnings("unchecked")
    protected final Object reverseConvertObject(final Object item) {
        if (isOriginalType(item)) {
            return reverseConvert((E) item);
        } else {
            return item;
        }
    }
    
    @Override
    public int size() {
        return wrapped.size();
    }
    
    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }
    
    @Override
    public boolean contains(final Object o) {
        return wrapped.contains(reverseConvertObject(o));
    }
    
    @Override
    public Iterator<E> iterator() {
        return new ConvertingIteratorWrapper(wrapped.iterator());
    }
    
    @Override
    public boolean add(final E e) {
        return wrapped.add(reverseConvert(e));
    }
    
    @Override
    public boolean remove(final Object o) {
        return wrapped.remove(reverseConvertObject(o));
    }
    
    @Override
    public boolean containsAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean addAll(final Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void clear() {
        wrapped.clear();
    }
    
    @Override
    public C unwrap() {
        return wrapped;
    }
    
    public class ConvertingIteratorWrapper implements Iterator<E>, Wrapper<Iterator<X>> {
        
        private final Iterator<X> wrapped;
        
        public ConvertingIteratorWrapper(final Iterator<X> wrapped) {
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
        public Iterator<X> unwrap() {
            return wrapped;
        }
        
    }
    
}
