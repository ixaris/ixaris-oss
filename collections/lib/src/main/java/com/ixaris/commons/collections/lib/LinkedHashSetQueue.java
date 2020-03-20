package com.ixaris.commons.collections.lib;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Queue;

public class LinkedHashSetQueue<E> extends LinkedHashSet<E> implements Queue<E> {
    
    private static final long serialVersionUID = -1803664267425426789L;
    
    @Override
    public final boolean offer(final E e) {
        return add(e);
    }
    
    @Override
    public final E remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        
        return poll();
    }
    
    @Override
    public final E poll() {
        if (isEmpty()) {
            return null;
        } else {
            final Iterator<E> i = iterator();
            E head = i.next();
            i.remove();
            return head;
        }
    }
    
    @Override
    public final E element() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        
        return peek();
    }
    
    @Override
    public final E peek() {
        if (isEmpty()) {
            return null;
        } else {
            return iterator().next();
        }
    }
    
}
