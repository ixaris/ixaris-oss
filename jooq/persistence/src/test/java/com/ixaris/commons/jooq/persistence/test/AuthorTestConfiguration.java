package com.ixaris.commons.jooq.persistence.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.ixaris.commons.jooq.persistence.TestDataSource;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.persistence.lib.datasource.MultiTenancyDataSourceConfig;
import com.ixaris.commons.persistence.lib.datasource.MultiTenantSchemaMigration;

@Configuration
@ImportResource("classpath*:spring/*.xml")
public class AuthorTestConfiguration {
    
    static final String UNIT_NAME = "jooq_persistence";
    
    @Bean
    public static TestDataSource defaultDataSource(final MultiTenancy multiTenancy,
                                                   final MultiTenancyDataSourceConfig config,
                                                   final Optional<MultiTenantSchemaMigration> migration) {
        // all we need to do is initialise the datasource. tenant lifecycle listeners will handle populating the
        // datasource.
        Set<String> units = Collections.singleton(UNIT_NAME);
        final TestDataSource dataSource = new TestDataSource("mysql", units, config, migration.orElse(null));
        multiTenancy.registerTenantLifecycleParticipant(dataSource);
        return dataSource;
    }
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
        return new PropertySourcesPlaceholderConfigurer();
    }
    
    @Bean
    public static ApplicationListener<ContextRefreshedEvent> installTestTenants(final MultiTenancy multiTenancy) {
        return event -> multiTenancy.setTenants(Arrays.asList(MultiTenancy.SYSTEM_TENANT, TestTenants.DEFAULT));
    }
    
}
