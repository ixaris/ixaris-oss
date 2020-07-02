package com.ixaris.commons.persistence.lib.idempotency;

import java.util.Optional;

import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.persistence.lib.exception.DuplicateIntentException;

/**
 * An interface representing a set of processed intents. Used to achieve idempotency.
 *
 * @author daniel.grech
 */
public interface ProcessedIntents {
    
    /**
     * Create an intent and persist it. This will mark the given intent as processed.
     *
     * @param intent The intent to mark as created
     * @throws RuntimeException if the intent is already processed.
     */
    void create(Intent intent) throws DuplicateIntentException;
    
    /**
     * Check whether an intent with the given id exists. If it exists, it means it has been processed.
     */
    boolean exists(Intent intent);
    
    /**
     * Fetch an intent with the given ID. Returns an {@link Optional}.
     */
    Optional<Intent> fetch(Intent intent);
    
    /**
     * Delete the intent with the given ID.
     */
    void delete(Intent intent);
}
