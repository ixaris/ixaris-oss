package com.ixaris.commons.jooq.persistence.flyway;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import org.junit.Test;

import com.ixaris.commons.jooq.persistence.JooqHikariTestHelper;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.persistence.lib.datasource.AbstractMigratableMultiTenantDataSource;

public class FlywayAutoRepairMultiTenantSchemaMigrationTest {
    
    public static final String UNIT_NAME = "jooq_persistence";
    
    @Test
    public void twoTenants_bothMigratedSuccessfully() {
        final JooqHikariTestHelper testHelper = new JooqHikariTestHelper(Collections.singleton(UNIT_NAME), TestTenants.LEFT, TestTenants.RIGHT);
        
        try {
            assertDataIsCorrectForTenant(testHelper.getDataSource(), TestTenants.LEFT);
            assertDataIsCorrectForTenant(testHelper.getDataSource(), TestTenants.RIGHT);
        } finally {
            testHelper.destroy();
        }
    }
    
    private void assertDataIsCorrectForTenant(final AbstractMigratableMultiTenantDataSource dataSource, final String tenant) {
        // missing tables will cause an exception - all tables should be initialised with 0 rows
        DATA_UNIT.exec(UNIT_NAME, () -> TENANT.exec(tenant, () -> {
            try (final Connection conn = dataSource.get().getConnection()) {
                final ResultSet resultSet = conn.createStatement().executeQuery("SELECT COUNT(*) FROM test_table");
                resultSet.next();
                assertThat(resultSet.getInt(1)).isEqualTo(1);
                conn.commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
