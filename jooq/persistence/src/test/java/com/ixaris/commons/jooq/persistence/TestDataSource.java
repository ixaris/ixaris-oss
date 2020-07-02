package com.ixaris.commons.jooq.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.mysql.cj.jdbc.MysqlDataSource;

import com.ixaris.commons.misc.lib.datasource.DataSourceWrapper;
import com.ixaris.commons.persistence.lib.datasource.AbstractMigratableMultiTenantDataSource;
import com.ixaris.commons.persistence.lib.datasource.MultiTenancyDataSourceConfig;
import com.ixaris.commons.persistence.lib.datasource.MultiTenantSchemaMigration;

public class TestDataSource extends AbstractMigratableMultiTenantDataSource {
    
    public TestDataSource(final String name,
                          final Set<String> units,
                          final MultiTenancyDataSourceConfig config,
                          final MultiTenantSchemaMigration migration) {
        super(name, "test", System.getProperty("spring.application.name"), units, config, migration);
    }
    
    @Override
    protected DataSource createShared(final Map<String, String> properties) {
        
        final MysqlDataSource ds = new MysqlDataSource();
        ds.setUrl(properties.get(URL_KEY));
        ds.setUser(properties.get(USER_KEY));
        ds.setPassword(properties.get(PASSWORD_KEY));
        
        return new DataSourceWrapper(ds) {
            
            @Override
            public Connection getConnection() throws SQLException {
                final Connection c = wrapped.getConnection();
                c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                c.setAutoCommit(false);
                return c;
            }
            
            @Override
            public Connection getConnection(final String username, final String password) throws SQLException {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    
}
