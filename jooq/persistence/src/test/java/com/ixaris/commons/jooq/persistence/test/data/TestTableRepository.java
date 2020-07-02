package com.ixaris.commons.jooq.persistence.test.data;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.TestTable.TEST_TABLE;

import java.util.Optional;

import com.ixaris.commons.persistence.lib.exception.EntityNotFoundException;

/**
 * Implements persistence layer for the entity Author. No idempotency implementation as it is a template for persistence proof of concept.
 */
public class TestTableRepository {
    
    public static TestTableEntity lookup(final String name) {
        return fetch(name).orElseThrow(EntityNotFoundException::new);
    }
    
    public static Optional<TestTableEntity> fetch(final String name) {
        return JOOQ_TX.get().fetchOptional(TEST_TABLE, TEST_TABLE.NAME.eq(name)).map(TestTableEntity::new);
    }
    
    private TestTableRepository() {}
    
}
