package com.ixaris.commons.jooq.persistence;

import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;

import java.util.Collections;

import org.junit.After;
import org.springframework.test.annotation.DirtiesContext;

import com.ixaris.commons.misc.lib.function.RunnableThrows;
import com.ixaris.commons.persistence.lib.AsyncPersistenceProvider;
import com.ixaris.commons.persistence.test.AbstractProcessedIntentsTest;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
@DirtiesContext
public class JooqProcessedIntentsTest extends AbstractProcessedIntentsTest {
    
    private static final String SERVICE_NAME = "jooq_persistence";
    
    private JooqHikariTestHelper testHelper;
    
    @Override
    protected void initialiseTenant(final String tenant) {
        System.setProperty("spring.application.name", SERVICE_NAME);
        testHelper = new JooqHikariTestHelper(Collections.singleton(SERVICE_NAME));
        testHelper.getMultiTenancy().addTenant(tenant);
    }
    
    @Override
    protected void exec(final RunnableThrows<? extends Exception> runnable) throws Exception {
        DATA_UNIT.exec(SERVICE_NAME, runnable);
    }
    
    @After
    public void teardown() {
        testHelper.destroy();
    }
    
    @Override
    protected AsyncPersistenceProvider createProvider() {
        return new JooqAsyncPersistenceProvider(JooqMultiTenancyConfiguration.createDslContext(testHelper.getDataSource()));
    }
    
}
