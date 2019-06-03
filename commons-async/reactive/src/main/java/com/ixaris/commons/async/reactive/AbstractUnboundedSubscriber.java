package com.ixaris.commons.async.reactive;

import com.ixaris.commons.misc.lib.logging.Logger;
import com.ixaris.commons.misc.lib.logging.LoggerFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A simple abstract subscriber that requests an unbounded number of messages
 *
 * @param <T>
 */
public abstract class AbstractUnboundedSubscriber<T> implements Subscriber<T> {
    
    private static final Logger LOG = LoggerFactory.forEnclosingClass();
    
    @Override
    public final void onSubscribe(final Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }
    
    @Override
    public void onError(final Throwable t) {
        LOG.atError(t).log("Stream terminated unexpectedly");
    }
    
    @Override
    public void onComplete() {}
    
}
