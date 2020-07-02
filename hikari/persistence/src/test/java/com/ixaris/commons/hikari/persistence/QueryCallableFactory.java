package com.ixaris.commons.hikari.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.lib.datasource.AbstractMultiTenantDataSource;

/**
 * @author benjie.gatt
 */
public class QueryCallableFactory {
    
    public static CallableThrows<String, SQLException> getStacheNameQueryCallable(final AbstractMultiTenantDataSource dataSource) {
        return getStacheNameQueryCallable(dataSource, "");
    }
    
    public static CallableThrows<String, SQLException> getStacheNameQueryCallable(final AbstractMultiTenantDataSource dataSource,
                                                                                  final String suffix) {
        return () -> {
            try (
                final Connection connection = dataSource.getConnection();
                final Statement statement = connection.createStatement()) {
                statement.execute("SELECT name FROM stache" + suffix);
                try (final ResultSet resultSet = statement.getResultSet()) {
                    resultSet.next();
                    return resultSet.getString(1);
                } finally {
                    connection.commit();
                }
            }
        };
    }
    
    public static CallableThrows<String, SQLException> getInsertDataCallable(final AbstractMultiTenantDataSource dataSource, final String name) {
        return getInsertDataCallable(dataSource, "", name);
    }
    
    public static CallableThrows<String, SQLException> getInsertDataCallable(final AbstractMultiTenantDataSource dataSource,
                                                                             final String suffix,
                                                                             final String name) {
        return () -> {
            try (
                final Connection connection = dataSource.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO stache" + suffix + " VALUES (?, ?, ?)")) {
                preparedStatement.setLong(1, UniqueIdGenerator.generate());
                preparedStatement.setString(2, name);
                preparedStatement.setLong(3, UniqueIdGenerator.generate());
                preparedStatement.executeUpdate();
                connection.commit();
            }
            return name;
        };
    }
}
