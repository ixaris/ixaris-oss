package com.ixaris.commons.async.reactive;

import org.reactivestreams.Subscription;

/**
 * An request strategy, used to abstract requesting more message from a publisher and to help reuse logic for applying backpressure.
 * 
 * E.g. can have a strategy that allows x messages per period of time, where one would (internally in the strategy) schedule invoking the request callable repeatedly
 * 
 * @author brian.vella
 */
public interface RequestStrategy {
    
    void add(final Subscription subscription);
    
    void remove(final Subscription subscription);
    
    /**
     * To be called as soon as onNext is invoked. Signals that a new message has been started
     */
    void startMessage(final Subscription subscription);
    
    /**
     * To be called as soon as a message has completed processing
     */
    void finishMessage(final Subscription subscription);
    
}
