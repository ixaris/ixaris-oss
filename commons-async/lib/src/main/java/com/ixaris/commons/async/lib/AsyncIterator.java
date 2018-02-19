package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.misc.lib.function.ConsumerThrows;

public interface AsyncIterator<E> {
    
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
    
    /**
     * @return a future resolved with the next element in the iteration
     *         or rejected with {@link java.util.NoSuchElementException} if the iteration has no more elements
     */
    Async<E> next() throws NoMoreElementsException;
    
    class NoMoreElementsException extends Exception {
        
    }
    
}
