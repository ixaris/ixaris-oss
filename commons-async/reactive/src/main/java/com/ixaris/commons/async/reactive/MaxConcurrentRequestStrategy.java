package com.ixaris.commons.async.reactive;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A request strategy that allows a maximum of concurrent messages. Requests 1 message for every completed message.
 * Users of this class should make sure that finishMessage is called when a message finishes processing, otherwise the
 * requests may become exhausted.
 *
 * @author brian.vella
 */
public final class MaxConcurrentRequestStrategy implements RequestStrategy {
    
    private final AtomicInteger available;
    
    public MaxConcurrentRequestStrategy(final int maxConcurrent) {
        available = new AtomicInteger(maxConcurrent);
    }
    
    @Override
    public final boolean startMessage() {
        return available.getAndUpdate(v -> (v > 0) ? (v - 1) : v) > 0;
    }
    
    @Override
    public final void finishMessage() {
        available.incrementAndGet();
    }
    
}
