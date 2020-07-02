package com.ixaris.commons.hikari.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Set;

import com.ixaris.commons.hikari.persistence.HikariMultiTenantDataSource;
import com.ixaris.commons.jooq.persistence.flyway.FlywayAutoRepairMultiTenantSchemaMigration;
import com.ixaris.commons.jooq.test.MultiTenancyTestDataSourceConfig;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.persistence.lib.datasource.AbstractMigratableMultiTenantDataSource;
import com.ixaris.commons.persistence.lib.datasource.MultiTenancyDataSourceConfig;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class JooqHikariTestHelper {
    
    private final MultiTenancyDataSourceConfig config;
    private final Set<String> units;
    private final AbstractMigratableMultiTenantDataSource dataSource;
    private final MultiTenancy multiTenancy;
    
    public JooqHikariTestHelper(final Set<String> units, final String... tenants) {
        this.config = MultiTenancyTestDataSourceConfig.multiTenancyDataSourceConfig();
        this.units = units;
        Arrays.stream(tenants).forEach(t -> units.forEach(u -> {
            try {
                clearDatabase(config, "test_" + u + '_' + t);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }));
        
        dataSource = new HikariMultiTenantDataSource("mysql",
            "test",
            System.getProperty("spring.application.name"),
            units,
            config,
            new FlywayAutoRepairMultiTenantSchemaMigration(),
            null,
            null);
        
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        multiTenancy.registerTenantLifecycleParticipant(dataSource);
    }
    
    public void destroy() {
        multiTenancy
            .getActiveTenants()
            .forEach(t -> units.forEach(u -> {
                try {
                    clearDatabase(config, "test_" + u + '_' + t);
                } catch (final Exception e) {
                    throw new IllegalStateException(e);
                }
            }));
        multiTenancy.stop();
    }
    
    public AbstractMigratableMultiTenantDataSource getDataSource() {
        return dataSource;
    }
    
    public MultiTenancyDataSourceConfig getConfig() {
        return config;
    }
    
    public MultiTenancy getMultiTenancy() {
        return multiTenancy;
    }
    
    public static void clearDatabase(final MultiTenancyDataSourceConfig config, final String databaseName) throws SQLException {
        try (
            final Connection connection = DriverManager.getConnection(config.getDefaults().getMysql().getUrl(),
                config.getDefaults().getMysql().getUser(),
                config.getDefaults().getMysql().getPassword());
            final Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS " + databaseName);
        }
    }
    
}
