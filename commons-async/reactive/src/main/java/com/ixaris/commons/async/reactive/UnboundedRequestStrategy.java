package com.ixaris.commons.async.reactive;

/**
 * Unbounded request strategy
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
