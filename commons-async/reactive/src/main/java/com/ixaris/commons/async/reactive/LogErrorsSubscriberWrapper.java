package com.ixaris.commons.async.reactive;

import com.ixaris.commons.misc.lib.logging.Logger;
import com.ixaris.commons.misc.lib.logging.LoggerFactory;
import com.ixaris.commons.misc.lib.object.Wrapper;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class LogErrorsSubscriberWrapper<T> implements Subscriber<T>, Wrapper<Subscriber<T>> {
    
    private static final Logger LOG = LoggerFactory.forEnclosingClass();
    
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
            LOG.atError(e).log("Error in onSubscribe");
        }
    }
    
    @Override
    public void onNext(final T t) {
        try {
            wrapped.onNext(t);
        } catch (final RuntimeException e) {
            LOG.atError(e).log("Error in onNext");
        }
    }
    
    @Override
    public void onComplete() {
        try {
            wrapped.onComplete();
        } catch (final RuntimeException e) {
            LOG.atError(e).log("Error in onComplete");
        }
    }
    
    @Override
    public void onError(final Throwable t) {
        try {
            wrapped.onError(t);
        } catch (final RuntimeException e) {
            LOG.atError(e).log("Error in onError");
        }
    }
    
    @Override
    public Subscriber<T> unwrap() {
        return wrapped;
    }
    
}
