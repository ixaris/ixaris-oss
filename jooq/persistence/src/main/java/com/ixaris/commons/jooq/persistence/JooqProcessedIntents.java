package com.ixaris.commons.jooq.persistence;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.isDuplicateEntryException;
import static com.ixaris.commons.jooq.persistence.jooq.tables.LibProcessedIntents.LIB_PROCESSED_INTENTS;

import java.util.Optional;

import org.jooq.exception.DataAccessException;

import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.jooq.persistence.jooq.tables.records.LibProcessedIntentsRecord;
import com.ixaris.commons.persistence.lib.exception.DuplicateIntentException;
import com.ixaris.commons.persistence.lib.exception.EntityNotFoundException;
import com.ixaris.commons.persistence.lib.idempotency.ProcessedIntents;

public class JooqProcessedIntents implements ProcessedIntents {
    
    @Override
    public void create(final Intent intent) throws DuplicateIntentException {
        try {
            final LibProcessedIntentsRecord record = JOOQ_TX.get().newRecord(LIB_PROCESSED_INTENTS);
            record.setIntentId(intent.getId());
            record.setPath(intent.getPath());
            record.setHash(intent.getHash());
            record.store();
        } catch (final DataAccessException e) {
            if (isDuplicateEntryException(e)) {
                throw new DuplicateIntentException("Duplicate intent " + intent.getId());
            }
            throw e;
        }
    }
    
    @Override
    public Optional<Intent> fetch(final Intent intent) {
        return JOOQ_TX.get()
            .fetchOptional(LIB_PROCESSED_INTENTS,
                (LIB_PROCESSED_INTENTS.INTENT_ID.equal(intent.getId()))
                    .and(LIB_PROCESSED_INTENTS.PATH.equal(intent.getPath()))
                    .and(LIB_PROCESSED_INTENTS.HASH.equal(intent.getHash())))
            .map(i -> new Intent(i.getIntentId(), i.getPath(), i.getHash()));
    }
    
    @Override
    public boolean exists(final Intent intent) {
        return fetch(intent).isPresent();
    }
    
    @Override
    public void delete(final Intent intent) {
        final int rowsDeleted = JOOQ_TX.get().executeDelete(new LibProcessedIntentsRecord(intent.getId(), intent.getPath(), intent.getHash()));
        
        if (rowsDeleted != 1) {
            throw new EntityNotFoundException(String.format("Entity with id [%s] does not exist", intent));
        }
    }
    
}
