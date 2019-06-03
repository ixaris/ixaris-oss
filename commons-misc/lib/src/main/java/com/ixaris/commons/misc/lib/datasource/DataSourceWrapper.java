package com.ixaris.commons.misc.lib.datasource;

import com.ixaris.commons.misc.lib.object.Wrapper;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import javax.sql.DataSource;

public abstract class DataSourceWrapper implements DataSource, Closeable, Wrapper<DataSource> {
    
    protected final DataSource wrapped;
    
    public DataSourceWrapper(final DataSource wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return wrapped.getConnection();
    }
    
    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        return wrapped.getConnection(username, password);
    }
    
    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        return wrapped.unwrap(iface);
    }
    
    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return wrapped.isWrapperFor(iface);
    }
    
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return wrapped.getLogWriter();
    }
    
    @Override
    public void setLogWriter(final PrintWriter out) throws SQLException {
        wrapped.setLogWriter(out);
    }
    
    @Override
    public void setLoginTimeout(final int seconds) throws SQLException {
        wrapped.setLoginTimeout(seconds);
    }
    
    @Override
    public int getLoginTimeout() throws SQLException {
        return wrapped.getLoginTimeout();
    }
    
    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return wrapped.getParentLogger();
    }
    
    @Override
    public void close() throws IOException {
        // this is a wrapper, so always implement closeable, but leave the actual implementation dependent on the
        // wrapped data source
        if (wrapped instanceof Closeable) {
            ((Closeable) wrapped).close();
        }
    }
    
    @Override
    public DataSource unwrap() {
        return wrapped;
    }
    
}
