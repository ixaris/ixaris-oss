package com.ixaris.commons.async.reactive;

import org.reactivestreams.Subscription;

/**
 * A simple abstract subscriber that requests an unbounded number of messages
 * 
 * @author brian.vella
 *
 * @param <T>
 */
public abstract class AbstractUnboundedSubscriber<T> extends AbstractSubscriber<T> {
    
    private Subscription subscription;
    
    @Override
    public synchronized final void onSubscribe(final Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }
    
    protected synchronized void cancel() {
        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
    }
}
