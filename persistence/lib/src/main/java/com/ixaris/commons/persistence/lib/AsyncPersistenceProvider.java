package com.ixaris.commons.persistence.lib;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncExecutor.execAndRelay;
import static com.ixaris.commons.misc.lib.exception.ExceptionUtil.sneakyThrow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.executor.AsyncExecutorServiceWrapper;
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;
import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.async.lib.thread.NamedThreadFactory;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.object.Wrapper;
import com.ixaris.commons.persistence.lib.exception.DuplicateIntentException;
import com.ixaris.commons.persistence.lib.exception.OptimisticLockException;
import com.ixaris.commons.persistence.lib.idempotency.ProcessedIntents;

/**
 * Asynchronous persistence provider that allows users to perform persistence related tasks asynchronously. The executor should be wrapped in an
 * AsyncLocalExecutor wrapper to preserve the async locals across executions. The result is relayed back to the originating executor if this is
 * wrapped by an AsyncExecutor wrapper, otherwise executed on the same thread.
 *
 * @author daniel.grech
 */
public abstract class AsyncPersistenceProvider implements AsyncInitiateTransaction {
    
    private static final int CORE_THREAD_POOL_SIZE = 10;
    private static final int MAX_THREAD_POOL_SIZE = 50;
    
    /**
     * The executor on which persistence tasks are executed.
     */
    private ExecutorService executor;
    
    public AsyncPersistenceProvider(final ExecutorService executor) {
        this.executor = Wrapper.isWrappedBy(executor, AsyncExecutorWrapper.class) ? executor : new AsyncExecutorServiceWrapper<>(executor);
    }
    
    public AsyncPersistenceProvider() {
        executor = new AsyncExecutorServiceWrapper<>(true, new ThreadPoolExecutor(CORE_THREAD_POOL_SIZE,
            MAX_THREAD_POOL_SIZE,
            2L,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            new NamedThreadFactory("AsyncPersistenceProvider-")));
    }
    
    public void shutdown() {
        executor.shutdown();
    }
    
    public abstract ProcessedIntents getProcessedIntents();
    
    protected abstract TransactionalPersistenceContext getContext();
    
    protected abstract <T, E extends Exception> T transaction(Transaction tx,
                                                              CallableThrows<T, E> persistenceCallable) throws E;
    
    public final AsyncInitiateTransaction optimisticLockRetry(final int retries) {
        return new AsyncInitiateTransaction() {
            
            @Override
            public <T, E extends Exception> Async<T> transaction(final CallableThrows<T, E> persistenceCallable) throws E {
                return AsyncPersistenceProvider.this.transaction(persistenceCallable, retries);
            }
            
            @Override
            public <T, E extends Exception> AsyncIntentTransaction<T, E> transaction(final Intent intent,
                                                                                     final CallableThrows<T, E> persistenceCallable) throws E {
                return AsyncPersistenceProvider.this.transaction(intent, persistenceCallable, retries);
            }
            
        };
    }
    
    /**
     * Persist in a transaction. Nested transactions are unsupported and will throw an {@link IllegalStateException}
     *
     * @param persistenceCallable The procedure performing the transactional persistence operation.
     * @param <T> The type of the persistence result.
     * @return A future which will contain the result of the persistence operation, once fulfilled.
     * @throws IllegalStateException if a transaction is already active
     */
    @Override
    public final <T, E extends Exception> Async<T> transaction(final CallableThrows<T, E> persistenceCallable) throws E {
        return transaction(persistenceCallable, 0);
    }
    
    /**
     * Persist in a transaction. The intent is also persisted transactionally.
     *
     * @param <T> The type of the persistence result.
     * @param intent An identifier used to distinguish between different operations.
     * @param persistenceCallable The callable performing the transactional persistence operation.
     * @return A future which will contain the result of the persistence operation, once fulfilled.
     * @throws IllegalStateException if a transaction is already active
     */
    public final <T, E extends Exception> AsyncIntentTransaction<T, E> transaction(final Intent intent,
                                                                                   final CallableThrows<T, E> persistenceCallable) throws E {
        return transaction(intent, persistenceCallable, 0);
    }
    
    public final <T, E extends Exception> Async<T> transaction(final Intent intent,
                                                               final CallableThrows<T, E> persistenceCallable,
                                                               final FunctionThrows<DuplicateIntentException, T, E> idempotencyFunction) throws E {
        return transaction(intent, persistenceCallable, 0, idempotencyFunction);
    }
    
    /**
     * Execute in a transaction. Creates a new transaction if one is not already active. When a transaction is already
     * present, the caller transaction logic should block() not await() on the result (which will not really block
     * since the future will always be fulfilled). WARNING care should be taken to not abuse this method. Use only when
     * a piece of logic can legitimately be used either in an existing transaction or in its own transaction. Explicit
     * transaction management should be used in most cases.
     *
     * @param persistenceCallable The procedure performing the transactional persistence operation.
     * @param <T> The type of the persistence result.
     * @return A future which will contain the result of the persistence operation, always fulfilled if transaction is
     *     already present.
     */
    public final <T, E extends Exception> Async<T> transactionRequired(final CallableThrows<T, E> persistenceCallable) throws E {
        if (getContext() == null) {
            return transaction(persistenceCallable, 0);
        } else {
            return result(persistenceCallable.call());
        }
    }
    
    private <T, E extends Exception> Async<T> transaction(final CallableThrows<T, E> callable,
                                                          final int optimisticLockRetries) throws E {
        validateNotNested();
        
        int retriesLeft = optimisticLockRetries;
        while (true) {
            final Transaction tx = new Transaction();
            try {
                return awaitExceptions(execAndRelay(executor, () -> transaction(tx, callable))).map(r -> {
                    // execute onCommit / onRollback after relaying back to computation executor
                    if (!tx.isRollbackOnly()) {
                        tx.executeOnCommit();
                    } else {
                        tx.executeOnRollback(null);
                    }
                    return r;
                });
            } catch (final OptimisticLockException e) {
                tx.executeOnRollback(e);
                if (retriesLeft > 0) {
                    retriesLeft--;
                } else {
                    throw e;
                }
            } catch (final Throwable t) { // NOSONAR we want to exec rollback functionality in any case
                tx.executeOnRollback(t);
                throw sneakyThrow(t);
            }
        }
    }
    
    private <T, E extends Exception> AsyncIntentTransaction<T, E> transaction(final Intent intent,
                                                                              final CallableThrows<T, E> persistenceCallable,
                                                                              final int optimisticLockRetries) throws E {
        return idempotencyFunction -> transaction(intent, persistenceCallable, optimisticLockRetries, idempotencyFunction);
    }
    
    private <T, E extends Exception> Async<T> transaction(final Intent intent,
                                                          final CallableThrows<T, E> persistenceCallable,
                                                          final int optimisticLockRetries,
                                                          final FunctionThrows<DuplicateIntentException, T, E> idempotencyFunction) throws E {
        if (intent != null) {
            validateNotNested();
            
            try {
                return transaction(
                    () -> {
                        try {
                            getProcessedIntents().create(intent);
                            return persistenceCallable.call();
                        } catch (final DuplicateIntentException e) {
                            return idempotencyFunction.apply(e);
                        }
                    },
                    optimisticLockRetries);
            } catch (final Exception e) {
                throw sneakyThrow(e);
            }
        } else {
            // unwrapping async object as otherwise fails when passing as parameter to generated lambda method
            return transaction(persistenceCallable, optimisticLockRetries);
        }
    }
    
    private void validateNotNested() {
        if (getContext() != null) {
            throw new IllegalStateException("Transaction already active. Nested transactions are unsupported.");
        }
    }
    
}
