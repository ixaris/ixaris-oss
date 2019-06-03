package com.ixaris.commons.async.reactive;

/**
 * An request strategy, used to abstract requesting more message from a publisher and to help reuse logic for applying
 * backpressure.
 *
 * <p>E.g. can have a strategy that allows x messages per period of time, where one would (internally in the strategy)
 * schedule invoking the request callable repeatedly
 *
 * @author brian.vella
 */
public interface RequestStrategy {
    
    /**
     * To be called as soon as a new message is to be started
     *
     * @return true if message can be handled, false to do backpressure
     */
    boolean startMessage();
    
    /**
     * To be called as soon as a message has completed processing
     */
    void finishMessage();
    
}
