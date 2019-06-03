package com.ixaris.commons.async.reactive;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A processor that asynchronously transforms a stream.
 *
 * @author brian.vella
 * @param <T>
 * @param <R>
 */
public final class TransformingProcessor<T, R> implements Processor<T, R> {
    
    private final Publisher<T> publisher;
    private final BiConsumer<T, Consumer<R>> consumer;
    private final AtomicReference<Subscriber<? super R>> subscriber = new AtomicReference<>();
    
    public TransformingProcessor(final Publisher<T> publisher, final BiConsumer<T, Consumer<R>> consumer) {
        this.publisher = publisher;
        this.consumer = consumer;
    }
    
    @Override
    public void onSubscribe(final Subscription s) {
        // subscriber is guaranteed to be non-null as this is called after subscribe() is called
        subscriber.get().onSubscribe(new TransformingSubscription(s));
    }
    
    @Override
    public void onNext(final T t) {
        consumer.accept(t, r -> subscriber.get().onNext(r));
    }
    
    @Override
    public void onError(final Throwable t) {
        subscriber.get().onError(t);
    }
    
    @Override
    public void onComplete() {
        subscriber.get().onComplete();
    }
    
    @Override
    public void subscribe(final Subscriber<? super R> s) {
        if (subscriber.compareAndSet(null, s)) {
            publisher.subscribe(this);
        } else {
            s.onSubscribe(DummySubscription.getInstance());
            s.onError(new IllegalStateException("Only 1 subscriber allowed"));
        }
    }
    
    private class TransformingSubscription implements Subscription {
        
        private final Subscription subscription;
        
        private TransformingSubscription(final Subscription subscription) {
            this.subscription = subscription;
        }
        
        @Override
        public void request(final long n) {
            subscription.request(n);
        }
        
        @Override
        public void cancel() {
            subscription.cancel();
        }
        
    }
}
