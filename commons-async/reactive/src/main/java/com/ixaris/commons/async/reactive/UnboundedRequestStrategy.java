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
    public final boolean startMessage() {
        return true;
    }
    
    @Override
    public final void finishMessage() {
        // no-op
    }
    
}
