package com.ixaris.commons.jooq.persistence;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.TestTable.TEST_TABLE;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.jooq.persistence.test.data.TestTableEntity;
import com.ixaris.commons.jooq.persistence.test.data.TestTableRepository;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.persistence.lib.AsyncInitiateTransaction;
import com.ixaris.commons.persistence.lib.AsyncPersistenceProvider;
import com.ixaris.commons.persistence.lib.exception.OptimisticLockException;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
@DirtiesContext
public class JooqOptimisticLockingTest {
    
    private static final String SERVICE_NAME = "jooq_persistence";
    
    private AsyncPersistenceProvider provider;
    private JooqHikariTestHelper testHelper;
    
    @Before
    public void setup() {
        testHelper = new JooqHikariTestHelper(Collections.singleton(SERVICE_NAME));
        testHelper.getMultiTenancy().addTenant(TestTenants.DEFAULT);
        provider = new JooqAsyncPersistenceProvider(JooqMultiTenancyConfiguration.createDslContext(testHelper.getDataSource()));
    }
    
    @After
    public void teardown() {
        testHelper.destroy();
    }
    
    @Test
    public void testLastUpdate_createAndUpdate_shouldSetLastUpdated() throws Throwable {
        final Long lastUpdated = AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                JOOQ_TX.get().deleteFrom(TEST_TABLE).execute();
                final TestTableEntity entity = new TestTableEntity("test");
                entity.apply(rec -> {
                    rec.setFlag1('X');
                    rec.setFlag2(true);
                    rec.setData("{}");
                });
                return entity.store().getRecord().getLastUpdated();
            })));
        
        assertThat(lastUpdated).isNotNull();
        
        final Long lastUpdatedAfterRead = AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                final TestTableEntity entity = TestTableRepository.lookup("test");
                return entity.getRecord().getLastUpdated();
            })));
        
        assertThat(lastUpdatedAfterRead).isEqualTo(lastUpdated);
        
        final Long lastUpdatedAfterUpdate = AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                final TestTableEntity entity = TestTableRepository.lookup("test");
                entity.apply(rec -> rec.setFlag1('Y'));
                return entity.store().getRecord().getLastUpdated();
            })));
        
        assertThat(lastUpdatedAfterUpdate).isGreaterThan(lastUpdatedAfterRead);
        
        final Long lastUpdatedAfterDirty = AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                final TestTableEntity entity = TestTableRepository.lookup("test");
                entity.apply(rec -> rec.setFlag1(rec.getFlag1()));
                return entity.store().getRecord().getLastUpdated();
            })));
        
        assertThat(lastUpdatedAfterDirty).isGreaterThan(lastUpdatedAfterUpdate);
        
        AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                JOOQ_TX.get().deleteFrom(TEST_TABLE).execute();
                return null;
            })));
    }
    
    @Test
    public void testOptimisticLocking() throws Throwable {
        AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                JOOQ_TX.get().deleteFrom(TEST_TABLE).execute();
                final TestTableEntity entity = new TestTableEntity("test_opt");
                entity.apply(rec -> {
                    rec.setFlag1('X');
                    rec.setFlag2(true);
                    rec.setData("{}");
                });
                entity.store();
                return null;
            })));
        
        final AtomicInteger sync = new AtomicInteger();
        final AtomicReference<Exception> exRef = new AtomicReference<>();
        final Thread t = new Thread(() -> exRef.set(doConcurrentUpdate("test_opt", '2', false, sync, true)));
        t.start();
        
        assertThat(doConcurrentUpdate("test_opt", '1', false, sync, false)).isNull();
        
        t.join();
        
        assertThat(exRef.get()).isInstanceOf(OptimisticLockException.class);
        
        final Character flag = AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                final TestTableEntity entity = TestTableRepository.lookup("test_opt");
                JOOQ_TX.get().deleteFrom(TEST_TABLE).execute();
                return entity.getRecord().getFlag1();
            })));
        
        assertThat(flag).isEqualTo('1');
    }
    
    @Test
    public void testOptimisticLockingSetDirty() throws Throwable {
        AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                JOOQ_TX.get().deleteFrom(TEST_TABLE).execute();
                final TestTableEntity entity = new TestTableEntity("test_opt_dirty");
                entity.apply(rec -> {
                    rec.setFlag1('X');
                    rec.setFlag2(true);
                    rec.setData("{}");
                });
                entity.store();
                return null;
            })));
        
        final AtomicInteger sync = new AtomicInteger();
        final AtomicReference<Exception> exRef = new AtomicReference<>();
        final Thread t = new Thread(() -> exRef.set(doConcurrentUpdate("test_opt_dirty", '2', false, sync, true)));
        t.start();
        
        assertThat(doConcurrentUpdate("test_opt_dirty", null, false, sync, false)).isNull();
        
        t.join();
        
        assertThat(exRef.get()).isInstanceOf(OptimisticLockException.class);
        
        final Character flag = AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                final TestTableEntity entity = TestTableRepository.lookup("test_opt_dirty");
                JOOQ_TX.get().deleteFrom(TEST_TABLE).execute();
                return entity.getRecord().getFlag1();
            })));
        
        assertThat(flag).isEqualTo('X');
    }
    
    @Test
    public void testOptimisticLockingRetry() throws Throwable {
        AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                JOOQ_TX.get().deleteFrom(TEST_TABLE).execute();
                final TestTableEntity entity = new TestTableEntity("test_opt_retry");
                entity.apply(rec -> {
                    rec.setFlag1('X');
                    rec.setFlag2(true);
                    rec.setData("{}");
                });
                entity.store();
                return null;
            })));
        
        final AtomicInteger sync = new AtomicInteger();
        final AtomicReference<Exception> exRef = new AtomicReference<>();
        final Thread t = new Thread(() -> exRef.set(doConcurrentUpdate("test_opt_retry", '2', true, sync, true)));
        t.start();
        
        assertThat(doConcurrentUpdate("test_opt_retry", '1', false, sync, false)).isNull();
        
        t.join();
        
        assertThat(exRef.get()).isNull();
        
        final Character flag = AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                final TestTableEntity entity = TestTableRepository.lookup("test_opt_retry");
                JOOQ_TX.get().deleteFrom(TEST_TABLE).execute();
                return entity.getRecord().getFlag1();
            })));
        
        assertThat(flag).isEqualTo('2');
    }
    
    @Test
    public void testOptimisticLockingAttach() throws Throwable {
        final TestTableEntity entity = AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                JOOQ_TX.get().deleteFrom(TEST_TABLE).execute();
                final TestTableEntity e = new TestTableEntity("test_opt_attach");
                e.apply(rec -> {
                    rec.setFlag1('X');
                    rec.setFlag2(true);
                    rec.setData("{}");
                });
                e.store();
                return e;
            })));
        
        AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                entity.apply(rec -> rec.setFlag1('Y')).store();
                return entity;
            })));
        
        final Character flag = AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .exec(() -> block(provider.transaction(() -> {
                final TestTableEntity e = TestTableRepository.lookup("test_opt_attach");
                JOOQ_TX.get().deleteFrom(TEST_TABLE).execute();
                return e.getRecord().getFlag1();
            })));
        
        assertThat(flag).isEqualTo('Y');
    }
    
    private Exception doConcurrentUpdate(final String name,
                                         final Character flag,
                                         final boolean withRetry,
                                         final AtomicInteger sync,
                                         final boolean await) {
        try {
            AsyncLocal
                .with(TENANT, TestTenants.DEFAULT)
                .with(DATA_UNIT, SERVICE_NAME)
                .exec(() -> {
                    final AsyncInitiateTransaction ti = withRetry ? provider.optimisticLockRetry(1) : provider;
                    return block(ti.transaction(() -> {
                        final TestTableEntity entity = TestTableRepository.lookup(name);
                        
                        sync.incrementAndGet();
                        while (sync.get() < (await ? 3 : 2)) {
                            Thread.sleep(10L);
                        }
                        JOOQ_TX.get().onCommit(sync::incrementAndGet);
                        if (flag != null) {
                            entity.apply(rec -> rec.setFlag1(flag));
                        } else {
                            entity.apply(rec -> rec.setFlag1(rec.getFlag1()));
                        }
                        entity.store();
                        return null;
                    }));
                });
            return null;
        } catch (final Exception e) {
            return e;
        }
    }
    
}
