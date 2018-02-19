package com.ixaris.commons.async.reactive;

import org.reactivestreams.Subscription;

/**
 * A request strategy that allows a maximum of concurrent messages. Requests 1 message for every completed message.
 * Users of this class should make sure that finishMessage is called when a message finishes processing, otherwise 
 * the requests may become exhausted.
 *  
 * @author brian.vella
 */
public final class MaxConcurrentRequestStrategy implements RequestStrategy {
    
    private final long maxConcurrent;
    
    public MaxConcurrentRequestStrategy(final long maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }
    
    @Override
    public final void add(final Subscription subscription) {
        if (subscription == null) {
            throw new IllegalArgumentException("subscription is null");
        }
        
        subscription.request(maxConcurrent);
    }
    
    @Override
    public void remove(final Subscription subscription) {
        // no-op
    }
    
    @Override
    public final void startMessage(final Subscription subscription) {
        // no-op
    }
    
    @Override
    public final void finishMessage(final Subscription subscription) {
        subscription.request(1L);
    }
    
}
