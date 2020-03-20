package com.ixaris.commons.multitenancy.lib.datasource;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;

import java.io.Closeable;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.ixaris.commons.misc.lib.conversion.Base64Util;
import com.ixaris.commons.misc.lib.datasource.DataSourceWrapper;
import com.ixaris.commons.multitenancy.lib.exception.InvalidMultiTenantObjectException;
import com.ixaris.commons.multitenancy.lib.object.AbstractEagerMultiTenantSharedObject;

/**
 * A wrapper object intended to hide the complexity of multitenancy when using datasources. Each instance of {@link
 * AbstractMultiTenantDataSource} represents a particular datasource connection for each tenant.
 *
 * <p>As long as there is an active tenant, this class can act as that tenant'sdatasource. All datasource actions require an active tenant.
 *
 * @author benjie.gatt
 */
public abstract class AbstractMultiTenantDataSource extends AbstractEagerMultiTenantSharedObject<DataSource, DataSource, Map<String, String>> implements DataSource {
    
    public static final String URL_KEY = "url";
    public static final String USER_KEY = "user";
    public static final String PASSWORD_KEY = "password"; // NOSONAR: this isn't a hardcoded password!
    
    protected static final class DataSourceWithSetCatalog extends DataSourceWrapper {
        
        private final Supplier<String> schemaNameSupplier;
        
        private DataSourceWithSetCatalog(final DataSource dataSource, final Supplier<String> schemaNameProducer) {
            super(dataSource);
            this.schemaNameSupplier = schemaNameProducer;
        }
        
        @Override
        public Connection getConnection() throws SQLException {
            final Connection connection = super.getConnection();
            connection.setCatalog(getSchemaName());
            return connection;
        }
        
        @Override
        public Connection getConnection(final String username, final String password) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public DataSource unwrap() {
            return wrapped;
        }
        
        public String getSchemaName() {
            return schemaNameSupplier.get();
        }
        
    }
    
    private final String prefix;
    private final String defaultUnit;
    private final Set<String> units;
    
    public AbstractMultiTenantDataSource(final String name, final String prefix, final String defaultUnit, final Set<String> units) {
        super(name);
        
        this.prefix = prefix;
        this.defaultUnit = defaultUnit;
        this.units = Collections.unmodifiableSet(units);
    }
    
    @Override
    protected String computeHash(final Map<String, String> properties) {
        verifyConfiguration(properties);
        return Base64Util.encode((properties.get(USER_KEY) + properties.get(PASSWORD_KEY) + properties.get(URL_KEY)).getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    protected DataSource wrap(final DataSource dataSource, final String tenantId) {
        return new DataSourceWithSetCatalog(dataSource, this::getCurrentSchemaName);
    }
    
    @Override
    protected void destroyShared(final DataSource dataSource) {
        if (dataSource instanceof Closeable) {
            try {
                ((Closeable) dataSource).close();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to close [" + dataSource + "]", e);
            }
        }
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return get().getConnection();
    }
    
    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        return get().getConnection(username, password);
    }
    
    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        return get().unwrap(iface);
    }
    
    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return get().isWrapperFor(iface);
    }
    
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return get().getLogWriter();
    }
    
    @Override
    public void setLogWriter(final PrintWriter out) throws SQLException {
        get().setLogWriter(out);
    }
    
    @Override
    public void setLoginTimeout(final int seconds) throws SQLException {
        get().setLoginTimeout(seconds);
    }
    
    @Override
    public int getLoginTimeout() throws SQLException {
        return get().getLoginTimeout();
    }
    
    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return get().getParentLogger();
    }
    
    private void verifyConfiguration(final Map<String, String> properties) {
        verifyProperty(properties, URL_KEY);
        verifyProperty(properties, USER_KEY);
        verifyProperty(properties, PASSWORD_KEY);
    }
    
    private void verifyProperty(final Map<String, String> properties, final String propertyKey) {
        if (properties.get(propertyKey) == null || properties.get(propertyKey).isEmpty()) {
            throw new InvalidMultiTenantObjectException("Missing " + propertyKey);
        }
    }
    
    protected Set<String> getUnits() {
        return units;
    }
    
    private String getCurrentSchemaName() {
        final String tenant = TENANT.get();
        if (tenant == null) {
            throw new IllegalStateException("Tenant not set. Did you exec within a TENANT.exec() block? "
                + "This is typically done by some aspect, infrastructure or framework");
        }
        final String unit = DATA_UNIT.get();
        //if (unit == null) {
        if ((unit == null) && (defaultUnit == null)) {
            throw new IllegalStateException("Data unit not set. Did you exec within a DATA_UNIT.exec() block? "
                + "This is typically done by some aspect, infrastructure or framework");
        }
        //        return prefix + "_" + unit + "_" + tenant;
        return prefix + "_" + (unit != null ? unit : defaultUnit) + "_" + tenant;
    }
    
}
