package com.ixaris.commons.hikari.persistence;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.ixaris.commons.jooq.persistence.flyway.FlywayAutoRepairMultiTenantSchemaMigration;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.data.DataUnit;
import com.ixaris.commons.persistence.lib.datasource.MultiTenancyDataSourceConfig;
import com.ixaris.commons.persistence.lib.datasource.MultiTenantSchemaMigration;

@Configuration
@ComponentScan({ "com.ixaris.commons.multitenancy.spring", "com.ixaris.commons.persistence.lib" })
public class DataSourceTestConfiguration {
    
    @Bean
    public static HikariMultiTenantDataSource defaultDataSource(@Value("${environment.name}") final String environment,
                                                                @Value("${spring.application.name}") final String serviceName,
                                                                final MultiTenancy multiTenancy,
                                                                final Optional<Set<DataUnit>> units,
                                                                final MultiTenancyDataSourceConfig config,
                                                                final Optional<MultiTenantSchemaMigration> migration) {
        // all we need to do is initialise the datasource. tenant lifecycle listeners will handle populating the
        // datasource.
        final HikariMultiTenantDataSource dataSource = new HikariMultiTenantDataSource("hikari",
            environment,
            serviceName,
            units.map(s -> s.stream().map(DataUnit::get).collect(Collectors.toSet())).orElse(Collections.emptySet()),
            config,
            migration.orElse(null),
            null,
            null);
        multiTenancy.registerTenantLifecycleParticipant(dataSource);
        return dataSource;
    }
    
    /**
     * Required for tests - production classes use Spring Boot, which has this automatically setup, but tests don't! without this, we would not
     * be able to have the configuration class read the properties via the ConfigurationProperties annotation
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
        return new PropertySourcesPlaceholderConfigurer();
    }
    
    @Bean
    public static MultiTenantSchemaMigration flywayMigrationStrategy() {
        return new FlywayAutoRepairMultiTenantSchemaMigration();
    }
    
    @Bean
    public static DataUnit hikariUnit() {
        return () -> "unit_hikari";
    }
    
}
