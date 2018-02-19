package com.ixaris.commons.async.reactive;

import org.reactivestreams.Publisher;

/**
 * Interface for implementing a publisher support mechanism. The publishing code uses a publisher support object to publish messages to a stream.
 * E.g. an actor publisher support sends published messages to a subscriber's mailbox.
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
     * @return false if the message cannot be published at this time due to backpressure
     */
    boolean next(T t);
    
    /**
     * Signal completion
     */
    void complete();
    
    /**
     * Signal fail
     * 
     * @param t the fail
     */
    void error(Throwable t);
    
}
