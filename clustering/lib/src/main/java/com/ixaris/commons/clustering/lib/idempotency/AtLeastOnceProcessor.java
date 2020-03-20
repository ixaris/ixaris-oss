package com.ixaris.commons.clustering.lib.idempotency;

/**
 * An At Least Once processor should provide guarantees that an operation is executed one or more times. Providing
 * exactly once guarantee is the responsibility of the implementation to avoid duplicate side-effects wherever
 * applicable.
 *
 * <p>At Least once processes can be further categorised into sub-types to simplify categorisation of pending
 * operations. Example usages of such sub-types are event publishing and the different event types are the sub-types.
 *
 * @author brian.vella
 * @author aldrin.seychell
 */
public interface AtLeastOnceProcessor {
    
    /**
     * Trigger poll now
     */
    void pollNow();
    
    /**
     * Stop processing of messages.
     */
    void stop();
    
}
