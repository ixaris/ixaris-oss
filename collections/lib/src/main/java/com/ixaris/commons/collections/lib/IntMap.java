package com.ixaris.commons.collections.lib;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ixaris.commons.collections.lib.IntMap.Entry;
import com.ixaris.commons.misc.lib.object.Wrapper;

public interface IntMap<T> extends Iterable<Entry<T>> {
    
    interface Entry<T> {
        
        int getKey();
        
        T getValue();
        
        T setValue(T value);
        
    }
    
    @SuppressWarnings("unchecked")
    static <T> IntMap<T> emptyMap() {
        return (IntMap<T>) EMPTY;
    }
    
    IntMap<?> EMPTY = new Empty<>();
    
    @SuppressWarnings("squid:S2972")
    final class Empty<T> implements IntMap<T> {
        
        @Override
        public int size() {
            return 0;
        }
        
        @Override
        public boolean isEmpty() {
            return true;
        }
        
        @Override
        public Iterator<Entry<T>> iterator() {
            return new Iterator<Entry<T>>() {
                
                @Override
                public boolean hasNext() {
                    return false;
                }
                
                @Override
                public Entry<T> next() {
                    throw new NoSuchElementException();
                }
                
            };
        }
        
        @Override
        public boolean containsValue(final T value) {
            return false;
        }
        
        @Override
        public boolean containsKey(final int key) {
            return false;
        }
        
        public T get(final int key) {
            return null;
        }
        
        @Override
        public T put(final int key, final T value) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public T remove(final int key) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
        
    }
    
    @SuppressWarnings("squid:S2972")
    final class Unmodifiable<T> implements IntMap<T>, Wrapper<IntMap<T>> {
        
        private final IntMap<T> wrapped;
        
        public Unmodifiable(final IntMap<T> wrapped) {
            this.wrapped = wrapped;
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
        public Iterator<Entry<T>> iterator() {
            return wrapped.iterator();
        }
        
        @Override
        public boolean containsValue(final T value) {
            return wrapped.containsValue(value);
        }
        
        @Override
        public boolean containsKey(final int key) {
            return wrapped.containsKey(key);
        }
        
        public T get(final int key) {
            return wrapped.get(key);
        }
        
        @Override
        public T put(final int key, final T value) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public T remove(final int key) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public IntMap<T> unwrap() {
            return wrapped;
        }
        
    }
    
    int size();
    
    boolean isEmpty();
    
    boolean containsValue(T value);
    
    boolean containsKey(int key);
    
    T get(int key);
    
    T put(int key, T value);
    
    T remove(int key);
    
    void clear();
    
}
