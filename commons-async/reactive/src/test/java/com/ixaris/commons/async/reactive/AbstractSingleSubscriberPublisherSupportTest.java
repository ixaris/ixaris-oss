package com.ixaris.commons.async.reactive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class AbstractSingleSubscriberPublisherSupportTest {
    
    private static class TestPublisherSupport extends AbstractSingleSubscriberPublisherSupport<Object> {
        
        @Override
        protected void next(final Subscriber<? super Object> subscriber, final Object t) {
            subscriber.onNext(t);
        }
        
    }
    
    private static class TestSubscriber implements Subscriber<Object> {
        
        private final List<Object> published = new ArrayList<>();
        private Subscription s;
        private boolean completed = false;
        private Throwable t;
        
        @Override
        public void onSubscribe(final Subscription s) {
            this.s = s;
        }
        
        @Override
        public void onNext(final Object t) {
            published.add(t);
        }
        
        @Override
        public void onError(final Throwable t) {
            this.t = t;
        }
        
        @Override
        public void onComplete() {
            this.completed = true;
        }
        
    }
    
    @Test
    public void testTwoSubscribers() {
        
        final TestPublisherSupport ps = new TestPublisherSupport();
        final TestSubscriber s1 = new TestSubscriber();
        final TestSubscriber s2 = new TestSubscriber();
        ps.subscribe(s1);
        ps.subscribe(s2);
        
        // assertEquals(ps, s1.s);
        // assertEquals(DummySubscription.getInstance(), s2.s);
        assertNotNull(s2.t);
        assertEquals(IllegalStateException.class, s2.t.getClass());
    }
    
}
