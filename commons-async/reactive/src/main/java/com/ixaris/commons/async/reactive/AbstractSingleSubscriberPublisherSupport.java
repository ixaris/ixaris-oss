package com.ixaris.commons.async.reactive;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Abstract publisher support that supports a single subscriber.
 * 
 * @author brian.vella
 *
 * @param <T>
 */
public abstract class AbstractSingleSubscriberPublisherSupport<T> implements PublisherSupport<T> {
    
    private final AtomicReference<SingleSubscription> subscription = new AtomicReference<>();
    
    public AbstractSingleSubscriberPublisherSupport() {}
    
    protected abstract void next(Subscriber<? super T> subscriber, T t);
    
    @Override
    public final void subscribe(final Subscriber<? super T> subscriber) {
        final SingleSubscription s = new SingleSubscription(subscriber);
        if (!subscription.compareAndSet(null, s)) {
            s.subscriber.onError(new IllegalStateException("Only 1 subscriber allowed"));
        }
    }
    
    @Override
    public final void next(final T t) {
        final SingleSubscription s = subscription.get();
        // if requested is equal to MAX_VALUE, then the subscriber requested unlimited items
        // otherwise decrement the number of requested items if positive
        if (s != null) {
            next(s.subscriber, t);
        } else {
            throw new IllegalStateException("no subscriber");
        }
    }
    
    private class SingleSubscription implements Subscription {
        
        private final Subscriber<? super T> subscriber;
        
        private SingleSubscription(final Subscriber<? super T> subscriber) {
            if (subscriber == null) {
                throw new IllegalArgumentException("subscriber is null");
            }
            
            this.subscriber = subscriber;
            this.subscriber.onSubscribe(this);
        }
        
        @Override
        public void request(long n) {
            if (n <= 0L) {
                throw new IllegalArgumentException("request should be positive, given " + n);
            }
            // ignore requests, we assume unbounded subscribers
        }
        
        @Override
        public void cancel() {
            subscription.compareAndSet(this, null);
        }
        
    }
    
}
