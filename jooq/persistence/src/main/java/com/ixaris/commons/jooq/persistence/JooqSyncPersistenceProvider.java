package com.ixaris.commons.jooq.persistence;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.persistence.lib.SyncPersistenceProvider;
import com.ixaris.commons.persistence.lib.Transaction;

/**
 * A {@link SyncPersistenceProvider} that performs multi-tenant persistence using JOOQ.
 *
 * @author armand.sciberras
 */
@Component
public final class JooqSyncPersistenceProvider extends SyncPersistenceProvider {
    
    private final DSLContext dslContext;
    private final JooqProcessedIntents processedIntents = new JooqProcessedIntents();
    
    @Autowired
    public JooqSyncPersistenceProvider(final DSLContext dslContext) {
        this.dslContext = dslContext;
    }
    
    @Override
    public JooqProcessedIntents getProcessedIntents() {
        return processedIntents;
    }
    
    @Override
    protected TransactionalDSLContext getContext() {
        return JOOQ_TX.get();
    }
    
    @Override
    protected <T, E extends Exception> T transaction(final Transaction tx, final CallableThrows<T, E> persistenceCallable) throws E {
        return JooqAsyncPersistenceProvider.transaction(dslContext, tx, persistenceCallable);
    }
    
}
