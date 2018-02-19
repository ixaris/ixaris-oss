package com.ixaris.commons.async.reactive;

import org.reactivestreams.Subscription;

/**
 * Unbounded request strategy
 *  
 * @author brian.vella
 */
public final class UnboundedRequestStrategy implements RequestStrategy {
    
    private static final UnboundedRequestStrategy INSTANCE = new UnboundedRequestStrategy();
    
    public static UnboundedRequestStrategy getInstance() {
        return INSTANCE;
    }
    
    private UnboundedRequestStrategy() {}
    
    @Override
    public void add(final Subscription subscription) {
        if (subscription == null) {
            throw new IllegalArgumentException("subscription is null");
        }
        
        subscription.request(Long.MAX_VALUE);
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
        // no-op
    }
    
}
