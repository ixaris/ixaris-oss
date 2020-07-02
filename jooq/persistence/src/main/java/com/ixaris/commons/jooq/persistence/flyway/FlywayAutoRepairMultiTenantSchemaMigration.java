package com.ixaris.commons.jooq.persistence.flyway;

import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import com.ixaris.commons.persistence.lib.datasource.MultiTenantSchemaMigration;

/**
 * Migration strategy that handles multi-tenant datasources. Will perform automatic schema updates on each tenant. Will be able to handle
 * previous migrations that failed - a Flyway repair is performed before actual migration.
 *
 * @author benjie.gatt
 */
@Component
public final class FlywayAutoRepairMultiTenantSchemaMigration implements MultiTenantSchemaMigration {
    
    static final String LOCATION = "classpath:db/migration/";
    
    public void migrate(final DataSource dataSource) {
        final Flyway flyway = Flyway
            .configure()
            .locations(LOCATION + DATA_UNIT.get())
            .dataSource(dataSource)
            .cleanDisabled(true) // like the comments say, doing a clean on production is a "career-limiting move"
            .schemas() // required to prevent flyway from reusing schema from previous tenant
            .load();
        
        // attempt a repair first in case previous migrations failed
        flyway.repair();
        flyway.migrate();
    }
    
}
