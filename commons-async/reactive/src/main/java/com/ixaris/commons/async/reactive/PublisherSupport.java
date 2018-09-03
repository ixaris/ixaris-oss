package com.ixaris.commons.async.reactive;

import org.reactivestreams.Publisher;

/**
 * Interface for implementing a publisher support mechanism. The publishing code uses a publisher support object to
 * publish messages to a stream.
 * 
 * @author brian.vella
 *
 * @param <T>
 */
public interface PublisherSupport<T> extends Publisher<T> {
    
    /**
     * publish a new message
     * 
     * @param t the message
     */
    void next(T t);
    
}
