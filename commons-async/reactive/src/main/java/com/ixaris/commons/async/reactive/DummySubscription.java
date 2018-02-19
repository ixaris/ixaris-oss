package com.ixaris.commons.async.reactive;

import org.reactivestreams.Subscription;

/**
 * Dummy subscription. To be used when rejecting subscribers, by supplying this no-op subscription and then calling onError()
 * 
 * @author brian.vella
 */
public class DummySubscription implements Subscription {
    
    private static final DummySubscription INSTANCE = new DummySubscription();
    
    public static DummySubscription getInstance() {
        return INSTANCE;
    }
    
    private DummySubscription() {}
    
    @Override
    public void request(final long n) {}
    
    @Override
    public void cancel() {}
    
}
