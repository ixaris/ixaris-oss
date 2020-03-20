package com.ixaris.commons.multitenancy.lib.datasource;

import static com.ixaris.commons.async.lib.Async.result;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.mockito.Mockito;

import com.ixaris.commons.async.lib.Async;

public class TestMultiTenancyDataSource extends AbstractMultiTenantDataSource {
    
    private static final Map<String, String> props = new HashMap<>();
    
    static {
        props.put(AbstractMultiTenantDataSource.URL_KEY, "url");
        props.put(AbstractMultiTenantDataSource.USER_KEY, "user");
        props.put(AbstractMultiTenantDataSource.PASSWORD_KEY, "pwd");
    }
    
    public TestMultiTenancyDataSource(final String name) {
        super(name, "test", null, Collections.emptySet());
    }
    
    @Override
    public Async<Void> preActivate(final String tenantId) {
        addTenant(tenantId, props);
        return result();
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
    
    @Override
    protected DataSource createShared(final Map<String, String> properties) {
        final DataSource dataSource = Mockito.mock(DataSource.class);
        final Connection connection = Mockito.mock(Connection.class);
        final Statement statement = Mockito.mock(Statement.class);
        try {
            Mockito.when(dataSource.getConnection()).thenReturn(connection);
            Mockito.when(connection.createStatement()).thenReturn(statement);
        } catch (SQLException e) {
            // ignored, never thrown when mocking
        }
        
        return dataSource;
    }
    
}
