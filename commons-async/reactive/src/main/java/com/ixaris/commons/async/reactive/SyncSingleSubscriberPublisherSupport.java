package com.ixaris.commons.async.reactive;

import org.reactivestreams.Subscriber;

public class SyncSingleSubscriberPublisherSupport<T> extends AbstractSingleSubscriberPublisherSupport<T> {
    
    @Override
    protected void next(final Subscriber<? super T> subscriber, final T t) {
        subscriber.onNext(t);
    }
    
}
