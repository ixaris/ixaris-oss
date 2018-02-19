package com.ixaris.commons.async.reactive;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.misc.lib.object.Wrapper;

public class LogErrorsSubscriberWrapper<T> implements Subscriber<T>, Wrapper<Subscriber<T>> {
    
    private static final Logger LOG = LoggerFactory.getLogger(LogErrorsSubscriberWrapper.class);
    
    private final Subscriber<T> wrapped;
    
    public LogErrorsSubscriberWrapper(final Subscriber<T> wrapped) {
        if (wrapped == null) {
            throw new IllegalArgumentException("wrapped is null");
        }
        
        this.wrapped = wrapped;
    }
    
    @Override
    public void onSubscribe(final Subscription s) {
        try {
            wrapped.onSubscribe(s);
        } catch (final RuntimeException e) {
            LOG.error("Error in onSubscribe", e);
        }
    }
    
    @Override
    public void onNext(final T t) {
        try {
            wrapped.onNext(t);
        } catch (final RuntimeException e) {
            LOG.error("Error in onNext", e);
        }
    }
    
    @Override
    public void onComplete() {
        try {
            wrapped.onComplete();
        } catch (final RuntimeException e) {
            LOG.error("Error in onComplete", e);
        }
    }
    
    @Override
    public void onError(final Throwable t) {
        try {
            wrapped.onError(t);
        } catch (final RuntimeException e) {
            LOG.error("Error in onError", e);
        }
    }
    
    @Override
    public Subscriber<T> unwrap() {
        return wrapped;
    }
    
}
