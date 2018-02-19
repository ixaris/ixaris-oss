package com.ixaris.commons.async.reactive;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPublisherSupport<T> implements PublisherSupport<T> {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPublisherSupport.class);
    
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Set<SubscriptionImpl> subscriptions = new HashSet<>();
    private final ReentrantReadWriteLock subscriptionsLock = new ReentrantReadWriteLock();
    private Throwable t;
    
    public AbstractPublisherSupport() {}
    
    protected abstract void next(Subscriber<? super T> subscriber, T t);
    
    protected abstract void complete(Subscriber<? super T> subscriber);
    
    protected abstract void error(Subscriber<? super T> subscriber, Throwable t);
    
    @Override
    public final void subscribe(final Subscriber<? super T> subscriber) {
        final SubscriptionImpl s = new SubscriptionImpl(subscriber);
        subscriber.onSubscribe(s);
        
        if (!closed.get()) {
            subscriptionsLock.writeLock().lock();
            try {
                subscriptions.add(s);
            } finally {
                subscriptionsLock.writeLock().unlock();
            }
        } else {
            if (t == null) {
                try {
                    complete(subscriber);
                } catch (final RuntimeException e) {
                    LOG.error("Error when calling complete", e);
                }
            } else {
                try {
                    error(subscriber, t);
                } catch (final RuntimeException e) {
                    LOG.error("Error when calling error", e);
                }
            }
        }
    }
    
    @Override
    public final boolean next(final T t) {
        final Set<SubscriptionImpl> updated = new HashSet<>();
        boolean ok = true;
        subscriptionsLock.readLock().lock();
        try {
            // check whether all subscribers can get this message
            for (final SubscriptionImpl s : subscriptions) {
                
                if (s.requested.get() > 0L) {
                    if (s.requested.get() < Long.MAX_VALUE) {
                        s.requested.decrementAndGet();
                        updated.add(s);
                    }
                } else {
                    LOG.warn("Unable to produce next message {} for subscription {} due to back-pressure", t, s);
                    ok = false;
                    break;
                }
            }
            
            // if not, reverse the ones updated and return false
            if (!ok) {
                for (final SubscriptionImpl s : updated) {
                    s.requested.incrementAndGet();
                }
                return false;
            }
            
            // publish
            for (final SubscriptionImpl s : subscriptions) {
                try {
                    next(s.subscriber, t);
                } catch (final RuntimeException e) {
                    LOG.error("Error when calling next", e);
                }
            }
            return true;
            
        } finally {
            subscriptionsLock.readLock().unlock();
        }
        
    }
    
    @Override
    public final void complete() {
        if (closed.compareAndSet(false, true)) {
            subscriptionsLock.writeLock().lock();
            try {
                final Iterator<SubscriptionImpl> i = subscriptions.iterator();
                while (i.hasNext()) {
                    try {
                        complete(i.next().subscriber);
                    } catch (final RuntimeException e) {
                        LOG.warn("Error when calling complete", e);
                    }
                    i.remove();
                }
            } finally {
                subscriptionsLock.writeLock().unlock();
            }
        } else {
            throw new IllegalStateException("Already closed");
        }
    }
    
    @Override
    public void error(final Throwable t) {
        if (closed.compareAndSet(false, true)) {
            this.t = t;
            subscriptionsLock.writeLock().lock();
            try {
                final Iterator<SubscriptionImpl> i = subscriptions.iterator();
                while (i.hasNext()) {
                    try {
                        error(i.next().subscriber, t);
                    } catch (final RuntimeException e) {
                        LOG.error("Error when calling error", e);
                    }
                    i.remove();
                }
            } finally {
                subscriptionsLock.writeLock().unlock();
            }
        } else {
            throw new IllegalStateException("Already closed");
        }
    }
    
    private class SubscriptionImpl implements Subscription {
        
        private final Subscriber<? super T> subscriber;
        private final AtomicLong requested = new AtomicLong(0L);
        
        public SubscriptionImpl(final Subscriber<? super T> subscriber) {
            if (subscriber == null) {
                throw new IllegalArgumentException("subscriber is null");
            }
            
            this.subscriber = subscriber;
        }
        
        @Override
        public void request(final long n) {
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
            subscriptionsLock.writeLock().lock();
            try {
                subscriptions.remove(this);
            } finally {
                subscriptionsLock.writeLock().unlock();
            }
        }
        
        @Override
        public String toString() {
            return subscriber.toString();
        }
    }
}
