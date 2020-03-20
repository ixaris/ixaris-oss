package com.ixaris.commons.collections.lib;

import com.ixaris.commons.misc.lib.object.Wrapper;

public interface BitSetIterator {
    
    final class Unmodifiable implements BitSetIterator, Wrapper<BitSetIterator> {
        
        private final BitSetIterator wrapped;
        
        public Unmodifiable(final BitSetIterator wrapped) {
            if (wrapped == null) {
                throw new IllegalArgumentException("wrapped is null");
            }
            this.wrapped = wrapped;
        }
        
        @Override
        public boolean hasNext() {
            return wrapped.hasNext();
        }
        
        @Override
        public int next() {
            return wrapped.next();
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public BitSetIterator unwrap() {
            return wrapped;
        }
        
    }
    
    boolean hasNext();
    
    int next();
    
    void remove();
    
}
