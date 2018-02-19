package com.ixaris.commons.async.reactive;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract publisher support that supports a single subscriber. If messages are published without a subscriber, they will be 
 * rejected in the same way as they are for backpressure. 
 * 
 * @author brian.vella
 *
 * @param <T>
 */
public abstract class AbstractSingleSubscriberPublisherSupport<T> implements PublisherSupport<T> {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSingleSubscriberPublisherSupport.class);
    
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<SingleSubscription> subscription = new AtomicReference<>();
    private Throwable t;
    
    public AbstractSingleSubscriberPublisherSupport() {}
    
    protected abstract void next(Subscriber<? super T> subscriber, T t);
    
    protected abstract void complete(Subscriber<? super T> subscriber);
    
    protected abstract void error(Subscriber<? super T> subscriber, Throwable t);
    
    @Override
    public final void subscribe(final Subscriber<? super T> subscriber) {
        final SingleSubscription s = new SingleSubscription(subscriber);
        if (subscription.compareAndSet(null, s)) {
            if (closed.get()) {
                if (t == null) {
                    complete(s.subscriber);
                } else {
                    error(s.subscriber, t);
                }
            }
        } else {
            error(s.subscriber, new IllegalStateException("Only 1 subscriber allowed"));
        }
    }
    
    @Override
    public final boolean next(final T t) {
        final SingleSubscription s = subscription.get();
        // if requested is equal to MAX_VALUE, then the subscriber requested unlimited items
        // otherwise decrement the number of requested items if positive
        if ((s != null) && s.requested.getAndUpdate(r -> (r == Long.MAX_VALUE) || (r == 0L) ? r : r - 1L) > 0L) {
            next(s.subscriber, t);
            return true;
        } else {
            LOG.warn("Unable to produce next message {} for subscription {} due to back-pressure", t, s);
            return false;
        }
    }
    
    @Override
    public final void complete() {
        if (closed.compareAndSet(false, true)) {
            final SingleSubscription s = subscription.get();
            if (s != null) {
                complete(s.subscriber);
            }
        } else {
            throw new IllegalStateException("Already closed");
        }
    }
    
    @Override
    public void error(final Throwable t) {
        if (closed.compareAndSet(false, true)) {
            this.t = t;
            final SingleSubscription s = subscription.get();
            if (s != null) {
                error(s.subscriber, t);
            }
        } else {
            throw new IllegalStateException("Already closed");
        }
    }
    
    private class SingleSubscription implements Subscription {
        
        private final Subscriber<? super T> subscriber;
        private final AtomicLong requested = new AtomicLong(0L);
        
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
            
            requested.getAndUpdate(r -> {
                final long nr = r + n;
                return (nr > r) ? nr : Long.MAX_VALUE;
            });
        }
        
        @Override
        public void cancel() {
            subscription.compareAndSet(this, null);
        }
        
    }
    
}
