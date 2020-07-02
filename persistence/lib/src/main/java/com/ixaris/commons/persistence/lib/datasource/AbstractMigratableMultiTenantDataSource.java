package com.ixaris.commons.persistence.lib.datasource;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.multitenancy.lib.datasource.AbstractMultiTenantDataSource;

/**
 * Each instance of {@link AbstractMigratableMultiTenantDataSource} represents one data source, abstracting away all the complexities of
 * multitenancy by having each tenant use this data source. The datasource itself detects which tenant is being used and picks up the relevant
 * connection accordingly.
 *
 * <p>Note: As a side-effect of this, all datasource actions require an active tenant (operations will fail if no tenant is found, as there is no
 * fall-back).
 *
 * @author benjie.gatt
 */
public abstract class AbstractMigratableMultiTenantDataSource extends AbstractMultiTenantDataSource {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMigratableMultiTenantDataSource.class);
    
    private final MultiTenancyDataSourceConfig config;
    private final MultiTenantSchemaMigration migration;
    
    public AbstractMigratableMultiTenantDataSource(final String name,
                                                   final String prefix,
                                                   final String defaultDataSourceUnit,
                                                   final Set<String> units,
                                                   final MultiTenancyDataSourceConfig config,
                                                   final MultiTenantSchemaMigration migration) {
        super(name, prefix, defaultDataSourceUnit, units);
        this.config = config;
        this.migration = migration;
    }
    
    @Override
    public Async<Void> preActivate(final String tenantId) {
        addTenant(tenantId, getTenantDatasourceProperties(tenantId));
        try {
            // the data source will handle the actual database, but we still need to prepare our data for use
            if (migration != null) {
                for (final String unit : getUnits()) {
                    AsyncLocal
                        .with(TENANT, tenantId)
                        .with(DATA_UNIT, unit)
                        .exec(() -> {
                            initIfRequired();
                            migration.migrate(this);
                        });
                }
            }
            return result();
        } catch (final RuntimeException e) {
            removeTenant(tenantId);
            // thinking behind this: its better that one tenant fails, than have the service stop working for all
            // tenants!
            LOG.error("Error preparing MySQL wrapped [{}] for tenant [{}]. Data may not be usable by tenant.", getName(), tenantId, e);
            throw e;
        }
    }
    
    @Override
    public Async<Void> activate(final String tenantId) {
        return result();
    }
    
    @Override
    public Async<Void> deactivate(final String tenantId) {
        return result();
    }
    
    @Override
    public Async<Void> postDeactivate(final String tenantId) {
        removeTenant(tenantId);
        return result();
    }
    
    private void initIfRequired() {
        // NOTE: this uses the raw datasource, so the connection is not bound to the tenant's schema.
        // This is fine for the CREATE DATABASE operation.
        final DataSource unwrapped = ((DataSourceWithSetCatalog) get()).unwrap();
        final String schemaName = ((DataSourceWithSetCatalog) get()).getSchemaName();
        try (
            final Connection conn = unwrapped.getConnection();
            final Statement stmt = conn.createStatement()) {
            LOG.info("Initialising database [{}] if required", schemaName);
            stmt.execute("CREATE DATABASE IF NOT EXISTS `" + schemaName + "`");
        } catch (final SQLException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private Map<String, String> getTenantDatasourceProperties(final String tenantId) {
        RelationalDbProperties db = null;
        // properties are in the form "multitenancy.mysql.[tenantId].[propertyName]=[value], so we need to split things
        // up first, given we don't know the tenants at compile-time
        if (config.getMysql() != null) {
            db = config.getMysql().get(tenantId);
        }
        if (db == null) {
            db = config.getDefaults().getMysql();
        }
        
        LOG.info("Falling back to default properties for tenant [{}]", tenantId);
        final Map<String, String> properties = new HashMap<>();
        properties.put(URL_KEY, db.getUrl());
        properties.put(USER_KEY, db.getUser());
        properties.put(PASSWORD_KEY, db.getPassword());
        return properties;
    }
    
}
