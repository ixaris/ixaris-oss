package com.ixaris.commons.collections.lib;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * helper to manage a set of listeners, with synchronisation to prevent exceptions if a listener adds or removes another
 * listener while handling an event (would cause {@link java.util.ConcurrentModificationException} if improperly
 * implemented.
 */
public final class ListenerSet<T> {
    
    private static final Logger LOG = LoggerFactory.getLogger(ListenerSet.class);
    
    private volatile ImmutableSet<T> listeners = ImmutableSet.of();
    
    @SuppressWarnings("squid:S1698")
    public synchronized boolean add(final T listener) {
        final ImmutableSet<T> copy = GuavaCollections.copyOfSetAdding(listeners, listener);
        if (copy == listeners) {
            return false;
        } else {
            listeners = copy;
            return true;
        }
    }
    
    @SuppressWarnings("squid:S1698")
    public synchronized boolean remove(final T listener) {
        final ImmutableSet<T> copy = GuavaCollections.copyOfSetRemoving(listeners, listener);
        if (copy == listeners) {
            return false;
        } else {
            listeners = copy;
            return true;
        }
    }
    
    public void publish(final Consumer<T> listenerConsumer) {
        final ImmutableSet<T> copy = listeners;
        for (final T listener : copy) {
            try {
                listenerConsumer.accept(listener);
            } catch (final RuntimeException e) {
                LOG.error("listener threw exception", e);
            }
        }
    }
    
    public int size() {
        return listeners.size();
    }
    
    public boolean isEmpty() {
        return listeners.isEmpty();
    }
    
}
