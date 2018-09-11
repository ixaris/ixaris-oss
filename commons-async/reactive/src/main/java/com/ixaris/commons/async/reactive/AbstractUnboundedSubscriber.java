package com.ixaris.commons.async.reactive;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple abstract subscriber that requests an unbounded number of messages
 * 
 * @author brian.vella
 *
 * @param <T>
 */
public abstract class AbstractUnboundedSubscriber<T> implements Subscriber<T> {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractUnboundedSubscriber.class);
    
    @Override
    public void onSubscribe(final Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }
    
    @Override
    public void onError(final Throwable t) {
        LOG.error("Stream terminated unexpectedly", t);
    }
    
    @Override
    public void onComplete() {}
    
}
