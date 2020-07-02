package com.ixaris.commons.persistence.lib;

import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.misc.lib.exception.ExceptionUtil;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.persistence.lib.exception.DuplicateIntentException;
import com.ixaris.commons.persistence.lib.exception.OptimisticLockException;
import com.ixaris.commons.persistence.lib.idempotency.ProcessedIntents;

/**
 * @author <a href="mailto:Armand.Sciberras@ixaris.com">Armand.Sciberras</a>
 */
public abstract class SyncPersistenceProvider implements InitiateTransaction {
    
    public abstract ProcessedIntents getProcessedIntents();
    
    protected abstract TransactionalPersistenceContext getContext();
    
    protected abstract <T, E extends Exception> T transaction(Transaction tx, CallableThrows<T, E> persistenceCallable) throws E;
    
    public final InitiateTransaction optimisticLockRetry(final int retries) {
        return new InitiateTransaction() {
            
            @Override
            public <T, E extends Exception> T transaction(final CallableThrows<T, E> persistenceCallable) throws E {
                return SyncPersistenceProvider.this.transaction(persistenceCallable, retries);
            }
            
            @Override
            public <T, E extends Exception> IntentTransaction<T, E> transaction(final Intent intent,
                                                                                final CallableThrows<T, E> persistenceCallable) throws E {
                return SyncPersistenceProvider.this.transaction(intent, persistenceCallable, retries);
            }
            
        };
    }
    
    /**
     * Persist in a transaction. Nested transactions are unsupported and will throw an {@link IllegalStateException}
     *
     * @param persistenceCallable The procedure performing the transactional persistence operation.
     * @param <T> The type of the persistence result.
     * @return The result of the persistence operation
     * @throws IllegalStateException if a transaction is already active
     */
    @Override
    public final <T, E extends Exception> T transaction(final CallableThrows<T, E> persistenceCallable) throws E {
        return transaction(persistenceCallable, 0);
    }
    
    /**
     * Persist in a transaction. The intent is also persisted transactionally.
     *
     * @param <T> The type of the persistence result.
     * @param intent An identifier used to distinguish between different operations.
     * @param persistenceCallable The callable performing the transactional persistence operation.
     * @return The result of the persistence operation
     * @throws IllegalStateException if a transaction is already active
     */
    @Override
    public final <T, E extends Exception> IntentTransaction<T, E> transaction(final Intent intent,
                                                                              final CallableThrows<T, E> persistenceCallable) throws E {
        return transaction(intent, persistenceCallable, 0);
    }
    
    /**
     * Execute in a transaction. Creates a new transaction if one is not already active. WARNING care should be taken to
     * not abuse this method. Use only when a piece of logic can legitimately be use either in an existing transaction 
     * or in its own transaction. Explicit transaction management should be used in most cases.
     *
     * @param persistenceCallable The procedure performing the transactional persistence operation.
     * @param <T> The type of the persistence result.
     * @return The result of the persistence operation
     */
    public final <T, E extends Exception> T transactionRequired(final CallableThrows<T, E> persistenceCallable) throws E {
        if (getContext() == null) {
            return transaction(persistenceCallable, 0);
        } else {
            return persistenceCallable.call();
        }
    }
    
    private <T, E extends Exception> T transaction(final CallableThrows<T, E> persistenceCallable,
                                                   final int optimisticLockRetries) throws E {
        if (getContext() != null) {
            throw new IllegalStateException("Transaction already active. Nested transactions are unsupported.");
        }
        
        int retriesLeft = optimisticLockRetries;
        while (true) {
            final Transaction tx = new Transaction();
            try {
                final T result = transaction(tx, persistenceCallable);
                if (!tx.isRollbackOnly()) {
                    tx.executeOnCommit();
                } else {
                    tx.executeOnRollback(null);
                }
                return result;
            } catch (final OptimisticLockException e) {
                tx.executeOnRollback(e);
                if (retriesLeft > 0) {
                    retriesLeft--;
                } else {
                    throw e;
                }
            } catch (final Throwable t) { // NOSONAR we want to exec rollback functionality in any case
                tx.executeOnRollback(t);
                throw ExceptionUtil.sneakyThrow(t);
            }
        }
    }
    
    private <T, E extends Exception> IntentTransaction<T, E> transaction(final Intent intent,
                                                                         final CallableThrows<T, E> persistenceCallable,
                                                                         final int optimisticLockRetries) throws E {
        if (intent != null) {
            return idempotencyFunction -> {
                try {
                    return transaction(() -> {
                        try {
                            getProcessedIntents().create(intent);
                            return persistenceCallable.call();
                        } catch (final DuplicateIntentException e) {
                            return idempotencyFunction.apply(e);
                        }
                    },
                        optimisticLockRetries);
                } catch (final Exception e) {
                    throw ExceptionUtil.sneakyThrow(e);
                }
            };
        } else {
            try {
                // unwrapping async object as otherwise fails when passing as parameter to generated lambda method
                final T result = transaction(persistenceCallable, optimisticLockRetries);
                return idempotencyFunction -> result;
            } catch (final Exception e) {
                return idempotencyFunction -> {
                    throw ExceptionUtil.sneakyThrow(e);
                };
            }
        }
    }
    
}
