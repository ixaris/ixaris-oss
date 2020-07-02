package com.ixaris.commons.hikari.persistence;

import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import com.ixaris.commons.persistence.lib.datasource.AbstractMigratableMultiTenantDataSource;
import com.ixaris.commons.persistence.lib.datasource.MultiTenancyDataSourceConfig;
import com.ixaris.commons.persistence.lib.datasource.MultiTenantSchemaMigration;

/**
 * Each instance of {@link AbstractMigratableMultiTenantDataSource} represents one data source, abstracting away all the complexities of
 * mulitenancy by having each tenant use this data source. The datasource itself detects which tenant is being used and picks up the relevant
 * connection accordingly.
 *
 * <p>Note: As a side-effect of this, all datasource actions require an active tenant (operations will fail if no tenant is found, as there is no
 * fall-back).
 *
 * @author benjie.gatt
 */
public final class HikariMultiTenantDataSource extends AbstractMigratableMultiTenantDataSource {
    
    // intentionally hard-coded. no need for an additional layer of complexity - if they need to be modified, have the
    // tenant specify them!
    private static final int MINIMUM_POOL_SIZE_DEFAULT = 1;
    private static final int MAXIMUM_POOL_SIZE_DEFAULT = 50;
    
    private final Object metricRegistry;
    private final Object healthCheckRegistry;
    
    public HikariMultiTenantDataSource(final String name,
                                       final String prefix,
                                       final String defaultDataSourceUnit,
                                       final Set<String> units,
                                       final MultiTenancyDataSourceConfig config,
                                       final MultiTenantSchemaMigration migration,
                                       final Object metricRegistry,
                                       final Object healthCheckRegistry) {
        super(name, prefix, defaultDataSourceUnit, units, config, migration);
        this.metricRegistry = metricRegistry;
        this.healthCheckRegistry = healthCheckRegistry;
    }
    
    @Override
    protected DataSource createShared(final Map<String, String> properties) {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
        // hikariConfig.setConnectionTestQuery("SELECT 1"); should not use with jdbc4 as this c
        // note: above comment found-as is. much like a dramatic death in a film, it looks just important enough to keep
        // without knowing the full context. potential ending could be: as this causes a round trip executing SELECT 1
        // every time the connection is used from the pool
        hikariConfig.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        
        hikariConfig.setMinimumIdle(MINIMUM_POOL_SIZE_DEFAULT);
        hikariConfig.setMaximumPoolSize(MAXIMUM_POOL_SIZE_DEFAULT);
        hikariConfig.setIdleTimeout(90000);
        
        hikariConfig.setUsername(properties.get(USER_KEY));
        hikariConfig.setPassword(properties.get(PASSWORD_KEY));
        hikariConfig.addDataSourceProperty(URL_KEY, properties.get(URL_KEY));
        hikariConfig.addDataSourceProperty(USER_KEY, properties.get(USER_KEY));
        hikariConfig.addDataSourceProperty(PASSWORD_KEY, properties.get(PASSWORD_KEY));
        
        // autocommit is set to true, otherwise some Jooq features will not work (notably, inserting via a TableRecord),
        // as the statement would not have been committed before a lookup
        // BV 6/3/2017 but transactions do not work with autocommit, so we should not use such features
        hikariConfig.setAutoCommit(false);
        
        // statement caching is intentionally not enabled. we are using JDBC's catalog feature to 'dynamically' set the
        // schema which is being retrieved. This has the side-effect of only working on the statement that will carry
        // out the query. with caching, we will have (cached) statements still holding a reference to the schema
        // (catalog) that was used when the statement was first created.
        
        hikariConfig.setMetricRegistry(metricRegistry);
        hikariConfig.setHealthCheckRegistry(healthCheckRegistry);
        hikariConfig.setPoolName(properties.get(URL_KEY));
        return new HikariDataSource(hikariConfig);
    }
    
}
