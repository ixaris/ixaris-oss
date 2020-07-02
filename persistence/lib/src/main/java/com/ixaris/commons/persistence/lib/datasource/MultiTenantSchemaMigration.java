package com.ixaris.commons.persistence.lib.datasource;

import javax.sql.DataSource;

/**
 * Migration strategy that handles multi-tenant datasources. Will perform automatic schema updates on each tenant. Will be able to handle
 * previous migrations that failed - a Flyway repair is performed before actual migration.
 *
 * @author benjie.gatt
 */
public interface MultiTenantSchemaMigration {
    
    void migrate(final DataSource dataSource);
    
}
