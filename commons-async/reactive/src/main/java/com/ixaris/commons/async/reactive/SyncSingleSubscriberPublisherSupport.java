package com.ixaris.commons.async.reactive;

import org.reactivestreams.Subscriber;

public class SyncSingleSubscriberPublisherSupport<T> extends AbstractSingleSubscriberPublisherSupport<T> {
    
    @Override
    protected void next(final Subscriber<? super T> subscriber, final T t) {
        subscriber.onNext(t);
    }
    
    @Override
    protected void complete(final Subscriber<? super T> subscriber) {
        subscriber.onComplete();
    }
    
    @Override
    protected void error(final Subscriber<? super T> subscriber, final Throwable t) {
        subscriber.onError(t);
    }
    
}
