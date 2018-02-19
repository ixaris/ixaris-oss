package com.ixaris.commons.async.reactive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        
        @Override
        protected void complete(final Subscriber<? super Object> subscriber) {
            subscriber.onComplete();
        }
        
        @Override
        protected void error(final Subscriber<? super Object> subscriber, final Throwable t) {
            subscriber.onError(t);
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
    public void testSuccessPath() {
        
        final TestPublisherSupport ps = new TestPublisherSupport();
        final TestSubscriber s = new TestSubscriber();
        ps.subscribe(s);
        
        // assertEquals(ps, s.s);
        assertEquals(0, s.published.size());
        assertFalse(ps.next(new Object()));
        
        s.s.request(1L);
        assertTrue(ps.next(new Object()));
        assertFalse(ps.next(new Object()));
        
        s.s.request(3L);
        assertTrue(ps.next(new Object()));
        assertTrue(ps.next(new Object()));
        assertTrue(ps.next(new Object()));
        assertFalse(ps.next(new Object()));
        assertEquals(4, s.published.size());
        
        ps.complete();
        assertTrue(s.completed);
    }
    
    @Test
    public void testErrorPath() {
        
        final TestPublisherSupport ps = new TestPublisherSupport();
        final TestSubscriber s = new TestSubscriber();
        ps.subscribe(s);
        
        // assertEquals(ps, s.s);
        assertEquals(0, s.published.size());
        assertFalse(ps.next(new Object()));
        
        RuntimeException r = new RuntimeException();
        ps.error(r);
        assertEquals(r, s.t);
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
