package com.ixaris.commons.clustering.lib.idempotency;

import java.util.List;

import com.ixaris.commons.async.lib.Async;

/**
 * A message processor that is responsible for executing/processing a message retrieved from some backing store.
 *
 * <p>The processor is designed to work for a specific message type since different message types would require a
 * different schema for storing messages and acknowledging
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public interface AtLeastOnceMessageType<T> {
    
    interface NextRetryTimeFunction {
        
        /**
         * @param failureCount the retry count, should be 1 for the first failure
         * @return the next retry time. Implementors may either return a fixed interval or exponential backoff
         */
        long calculate(final int failureCount);
        
    }
    
    String getKey();
    
    /**
     * Some queues require strict ordering. Ordering is based on sequence number, shard and subtype. If this setting is
     * true, should a message fail, all messages with the same shard and subtype and with a greater sequence number will
     * be removed from the queue. Otherwise, the failures are simply skipped. It is the responsibility of implementors
     * to respect this same logic when fetching pending messages, i.e. if a queue is blocked, subsequent calls to
     * pending() should not return the failed message until it it's retry time is reached.
     *
     * @return true to skip over failed messages, false otherwise
     */
    default boolean isFailedMessageBlocksQueue() {
        return false;
    }
    
    /**
     * Loads a batch of pending messages except those to be retried later than the specified timestamp. Implementors are
     * responsible for omitting blocked messages, especially messages that are blocked by an earlier failed message in
     * the same shard.
     *
     * <p>This method MUST NOT block! Any blocking operations need to be performed on a separate thread and complete the
     * returned future
     *
     * @return future completing with a list of pending messages
     */
    Async<PendingMessages<T>> pending(final long timestamp);
    
    /**
     * Attempt to process a message. Implementors are responsible for persisting immutability data, as well as failure
     * counts and next retry time obtained from the given function
     *
     * <p>This method MUST NOT block! Any blocking operations needs to be performed on a separate thread and complete
     * the returned future
     *
     * @param pendingMessage The message to be processed
     * @param nextRetryTimeFunction
     * @return future completed when successfully processing a message
     */
    Async<Void> processMessage(StoredPendingMessage<T> pendingMessage, NextRetryTimeFunction nextRetryTimeFunction);
    
}
