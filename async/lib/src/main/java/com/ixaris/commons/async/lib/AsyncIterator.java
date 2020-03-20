package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncExecutor.yield;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import com.ixaris.commons.misc.lib.function.ConsumerThrows;
import com.ixaris.commons.misc.lib.object.Wrapper;

public interface AsyncIterator<E> {
    
    @SuppressWarnings("squid:S1166")
    static <T, E extends Exception> Async<Void> forEach(final AsyncIterator<T> iterator, ConsumerThrows<T, E> consumer) throws E {
        boolean done = false;
        while (!done) {
            try {
                consumer.accept(await(iterator.next()));
            } catch (final NoMoreElementsException e) {
                done = true;
            }
        }
        return result();
    }
    
    static <T> T closeableNext(final Iterator<T> iterator, final Closeable closeable) throws NoMoreElementsException {
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            try {
                closeable.close();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            throw new NoMoreElementsException();
        }
    }
    
    /**
     * @return a future resolved with the next element in the iteration or rejected with {@link
     *     java.util.NoSuchElementException} if the iteration has no more elements
     */
    Async<E> next() throws NoMoreElementsException;
    
    class NoMoreElementsException extends Exception {}
    
    final class YieldingAsyncIterator<E> implements AsyncIterator<E>, Wrapper<AsyncIterator<E>> {
        
        private final AsyncIterator<E> wrapped;
        private final int yieldEvery;
        private int countUntilYield;
        
        public YieldingAsyncIterator(final AsyncIterator<E> wrapped, final int yieldEvery) {
            if (wrapped == null) {
                throw new IllegalArgumentException("wrapped is null");
            }
            if (yieldEvery < 1) {
                throw new IllegalArgumentException("yieldEvery is < 1");
            }
            
            this.wrapped = wrapped;
            this.yieldEvery = yieldEvery;
        }
        
        public YieldingAsyncIterator(final AsyncIterator<E> wrapped) {
            this(wrapped, 1);
        }
        
        @Override
        public Async<E> next() throws NoMoreElementsException {
            if (countUntilYield >= yieldEvery) {
                countUntilYield = 0;
                await(yield());
            }
            countUntilYield++;
            return wrapped.next();
        }
        
        @Override
        public AsyncIterator<E> unwrap() {
            return wrapped;
        }
        
    }
    
}
