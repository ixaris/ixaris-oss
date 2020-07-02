package com.ixaris.commons.jooq.persistence;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;

import com.ixaris.commons.async.lib.thread.ThreadLocalHelper;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.persistence.lib.AsyncPersistenceProvider;
import com.ixaris.commons.persistence.lib.Transaction;

/**
 * A {@link AsyncPersistenceProvider} that performs multi-tenant persistence using JOOQ.
 *
 * @author tiago.cucki
 */
public final class JooqAsyncPersistenceProvider extends AsyncPersistenceProvider {
    
    private final DSLContext dslContext;
    private final JooqProcessedIntents processedIntents = new JooqProcessedIntents();
    
    @Autowired
    public JooqAsyncPersistenceProvider(final DSLContext dslContext) {
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
        return transaction(dslContext, tx, persistenceCallable);
    }
    
    @SuppressWarnings("unchecked")
    static <T, E extends Exception> T transaction(final DSLContext dslContext,
                                                  final Transaction tx,
                                                  final CallableThrows<T, E> persistenceCallable) throws E {
        try {
            return dslContext.transactionResult(configuration -> {
                configuration.settings().withExecuteWithOptimisticLocking(true).withExecuteWithOptimisticLockingExcludeUnversioned(true);
                final TransactionalDSLContext ctx = TransactionalDSLContext.create(tx, DSL.using(configuration));
                final T result = ThreadLocalHelper.exec(JOOQ_TX, ctx, persistenceCallable);
                if (tx.isRollbackOnly()) {
                    throw new TriggerRollbackException(result);
                }
                return result;
            });
        } catch (final DataAccessException e) {
            throw TransactionalDSLContext.handle(e);
        } catch (final TriggerRollbackException e) {
            return (T) e.result;
        }
    }
    
    private static final class TriggerRollbackException extends RuntimeException {
        
        private final Object result;
        
        private TriggerRollbackException(final Object result) {
            this.result = result;
        }
        
    }
    
}
