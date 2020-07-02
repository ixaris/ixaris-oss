package com.ixaris.commons.persistence.lib;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.async.lib.thread.ThreadLocalHelper;
import com.ixaris.commons.async.test.CompletionStageAssert;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.persistence.lib.exception.DuplicateIntentException;
import com.ixaris.commons.persistence.lib.idempotency.ProcessedIntents;

/**
 * Tests for the {@link AsyncPersistenceProvider}, specifically, the transaction execution functionality.
 *
 * @author daniel.grech
 */
public class AsyncPersistenceProviderTest {
    
    private static final long DEFAULT_TIMEOUT = 5000L;
    private static final Intent INTENT = new Intent(123, "test", 123);
    
    private static MultiTenancy multiTenancy;
    
    private DummyAsyncPersistenceProvider asyncProvider;
    
    @BeforeClass
    public static void setupClass() {
        multiTenancy = new MultiTenancy();
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.start();
    }
    
    @AfterClass
    public static void teardownClass() {
        multiTenancy.stop();
    }
    
    @Before
    public void setup() {
        asyncProvider = new DummyAsyncPersistenceProvider();
    }
    
    @Test
    public void performAsyncTask_TaskPerformedAfterPromiseReturnedSuccessfully() throws Exception {
        final AtomicInteger ii = new AtomicInteger();
        final CompletionStage<Integer> stage = asTenant(TestTenants.DEFAULT, () -> asyncProvider.transaction(ii::incrementAndGet));
        
        CompletionStageAssert.assertThat(stage).await().isFulfilled().isEqualTo(1);
    }
    
    @Test
    public void performMultipleAsyncTasks_AllTasksPerformedSuccessfully() {
        final AtomicInteger ii = new AtomicInteger();
        final int numTestCases = 10;
        final ExecutorService executorService = Executors.newFixedThreadPool(numTestCases);
        for (int i = 0; i < numTestCases; i++) {
            executorService.submit(() -> asTenant(TestTenants.DEFAULT, () -> asyncProvider.transaction(ii::incrementAndGet)));
        }
        
        // Instead of waiting on all promises manually, just wait a set timeout for the atomic integer inside the
        // context to reach the expected value
        boundedBusyWaitUntilConditionSatisfied(() -> ii.get() == numTestCases,
            "Expected counter to be incremented ten times",
            DEFAULT_TIMEOUT);
    }
    
    @Test
    public void performAsyncTask_TaskFails_PromiseIsReturnedWithException() throws Exception {
        final CompletionStage<Object> stage = asTenant(TestTenants.DEFAULT, () -> asyncProvider.transaction(() -> {
            throw new RuntimeException("test");
        }));
        
        CompletionStageAssert.assertThat(stage).await().isRejectedWith(RuntimeException.class).hasMessageContaining("test");
    }
    
    @Test
    public void transactionRequired_noOpenTransaction_shouldCreateNewTransaction() throws Exception {
        join(asTenant(TestTenants.DEFAULT, () -> asyncProvider.transactionRequired(() -> Assertions.assertThat(asyncProvider.getContext()).isNotNull())));
    }
    
    @Test
    public void transactionRequired_alreadyOpenTransaction_shouldNotCreateNewTransaction() throws Exception {
        join(asTenant(TestTenants.DEFAULT, () -> asyncProvider.transactionRequired(() -> {
            final DummyPersistenceContext context = asyncProvider.getContext();
            return asyncProvider.transactionRequired(() -> Assertions.assertThat(asyncProvider.getContext()).isEqualTo(context));
        })));
    }
    
    @Test
    public void transaction_alreadyOpenTransaction_shouldNotAllowNestedTx() throws Exception {
        join(asTenant(TestTenants.DEFAULT, () -> asyncProvider.transaction(() -> {
            Assertions
                .assertThatThrownBy(() -> asyncProvider.transaction(() -> {
                    fail("Should not allow nested tx");
                    return null;
                }))
                .isInstanceOf(IllegalStateException.class);
            return null;
        })));
    }
    
    @Test
    public void transaction_withIntent_alreadyOpenTransaction_shouldNotAllowNestedTx() throws Exception {
        join(asTenant(TestTenants.DEFAULT, () -> asyncProvider.transaction(INTENT, () -> {
            Assertions
                .assertThatThrownBy(() -> asyncProvider.transaction(INTENT, () -> {
                    fail("Should not allow nested tx");
                    return null;
                }).throwOnDuplicateIntent())
                .isInstanceOf(IllegalStateException.class);
            return null;
        })).throwOnDuplicateIntent());
    }
    
    @Test
    public void transactionRequired_nestedTransactions_secondNestedTransactionFails_shouldRollbackOneTransaction() throws Exception {
        final int before = asyncProvider.getRolledBackTxCount().get();
        
        CompletionStageAssert
            .assertThat(asTenant(TestTenants.DEFAULT, () -> asyncProvider.transactionRequired(() -> {
                join(asyncProvider.transactionRequired(() -> false));
                join(asyncProvider.transactionRequired(() -> {
                    throw new IllegalStateException();
                }));
                return 0;
            })))
            .await()
            .isRejectedWith(IllegalStateException.class);
        
        Assertions.assertThat(asyncProvider.getRolledBackTxCount().get() - before).isOne();
    }
    
    @Test
    public void transaction_nestedTransactions_secondNestedTransactionFails_shouldRollbackTwoTransactions() {
        final int beforeTx = asyncProvider.getTxCount().get();
        final int beforeRollback = asyncProvider.getRolledBackTxCount().get();
        
        CompletionStageAssert
            .assertThat(() -> asTenant(TestTenants.DEFAULT, () -> asyncProvider.transaction(() -> {
                join(asyncProvider.transactionRequired(() -> false));
                join(asyncProvider.transactionRequired(() -> {
                    throw new IllegalStateException();
                }));
                return result(0);
            })))
            .await()
            .isRejectedWith(IllegalStateException.class);
        
        Assertions.assertThat(asyncProvider.getTxCount().get() - beforeTx).isEqualTo(1);
        Assertions.assertThat(asyncProvider.getRolledBackTxCount().get() - beforeRollback).isEqualTo(1);
    }
    
    private <V> V asTenant(final String tenant, final CallableThrows<V, Exception> task) throws Exception {
        return TENANT.exec(tenant, task);
    }
    
    @SuppressWarnings("BusyWait")
    private static void boundedBusyWaitUntilConditionSatisfied(final Supplier<Boolean> condition, final String description, final long timeoutMs) {
        final long timeout = System.currentTimeMillis() + timeoutMs;
        try {
            while (System.currentTimeMillis() < timeout) {
                if (condition.get()) {
                    return;
                }
                Thread.sleep(100L);
            }
        } catch (final InterruptedException ignored) {
            // do nothing
        }
        
        if (!condition.get()) {
            throw new AssertionError(String.format("Expected condition [%s] to be true after timeout [%d ms] but it was not",
                description,
                timeoutMs));
        }
    }
    
    /**
     * Dummy persistence context - for test purposes
     */
    private static class DummyPersistenceContext implements TransactionalPersistenceContext {
        
        private boolean rollbackOnly = false;
        
        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }
        
        @Override
        public void setRollbackOnly() {
            rollbackOnly = true;
        }
        
        @Override
        public void onCommit(final Runnable runnable) {}
        
        @Override
        public void onRollback(final Consumer<Throwable> consumer) {}
        
    }
    
    private static class DummyProcessedIntents implements ProcessedIntents {
        
        private List<Intent> processedIntents = new LinkedList<>();
        
        @Override
        public void create(final Intent intent) throws DuplicateIntentException {
            if (processedIntents.contains(intent)) {
                throw new DuplicateIntentException();
            } else {
                processedIntents.add(intent);
            }
        }
        
        @Override
        public boolean exists(final Intent intent) {
            return processedIntents.contains(intent);
        }
        
        @Override
        public Optional<Intent> fetch(final Intent intent) {
            return Iterables.tryFind(processedIntents, i -> i.equals(intent)).toJavaUtil();
        }
        
        @Override
        public void delete(final Intent intent) {
            Iterables.removeAll(processedIntents, singleton(intent));
        }
    }
    
    /**
     * Dummy persistence provider - for test purposes
     */
    private static class DummyAsyncPersistenceProvider extends AsyncPersistenceProvider {
        
        private final ThreadLocal<DummyPersistenceContext> TX_CTX = new ThreadLocal<>();
        private final AtomicInteger transactionCount = new AtomicInteger(0);
        private final AtomicInteger rolledBackTransactionCount = new AtomicInteger(0);
        private final DummyProcessedIntents processedIntents = new DummyProcessedIntents();
        
        @Override
        protected <T, E extends Exception> T transaction(final Transaction tx, final CallableThrows<T, E> callable) throws E {
            return ThreadLocalHelper.exec(TX_CTX, new DummyPersistenceContext(), () -> {
                transactionCount.incrementAndGet();
                tx.onRollback(e -> rolledBackTransactionCount.getAndIncrement());
                return callable.call();
            });
        }
        
        @Override
        public DummyPersistenceContext getContext() {
            return TX_CTX.get();
        }
        
        @Override
        public DummyProcessedIntents getProcessedIntents() {
            return processedIntents;
        }
        
        public AtomicInteger getTxCount() {
            return transactionCount;
        }
        
        public AtomicInteger getRolledBackTxCount() {
            return rolledBackTransactionCount;
        }
    }
}
