package com.ixaris.commons.hikari.persistence;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import com.ixaris.commons.misc.spring.EnvDefaults;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.data.DataUnit;
import com.ixaris.commons.persistence.lib.datasource.MultiTenancyDataSourceConfig;
import com.ixaris.commons.persistence.lib.datasource.MultiTenantSchemaMigration;

@Configuration
public class MysqlConfiguration {
    
    private static final String MYSQL_SCHEMA_PREFIX_PROP = "mysql.schemaPrefix";
    
    @Bean
    @Primary
    public static HikariMultiTenantDataSource mysqlDefaultDataSource(@Value("${spring.application.name}") final String serviceName,
                                                                     @Value("${environment.name}") final String envName,
                                                                     final MultiTenancy multiTenancy,
                                                                     final Environment environment,
                                                                     final Optional<Set<DataUnit>> units,
                                                                     final MultiTenancyDataSourceConfig config,
                                                                     final MultiTenantSchemaMigration migration) {
        // all we need to do is initialise the datasource. tenant lifecycle listeners will handle populating the
        // datasource.
        final HikariMultiTenantDataSource ds = new HikariMultiTenantDataSource("mysql",
            getMysqlSchemaPrefix(environment, envName),
            serviceName,
            units.orElse(Collections.emptySet()).stream().map(DataUnit::get).collect(Collectors.toSet()),
            config,
            migration,
            null,
            null);
        multiTenancy.registerTenantLifecycleParticipant(ds);
        return ds;
    }
    
    static String getMysqlSchemaPrefix(final Environment env, final String defaultValue) {
        return EnvDefaults.getOrDefault(env, MYSQL_SCHEMA_PREFIX_PROP, defaultValue);
    }
    
}
