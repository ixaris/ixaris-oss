package com.ixaris.commons.microservices.defaults.test.local;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.sql.DataSource;

import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Schema;
import org.jooq.TransactionProvider;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.ThreadLocalTransactionProvider;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;

import com.ixaris.commons.clustering.lib.extra.LocalCluster;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;

/**
 * Configuration used for unit tests that provides: - In memory database (H2) - Jooq async persistence - Multitenancy
 */
@ComponentScan("com.ixaris.commons.jooq.persistence")
public class UnitTestConfiguration {
    
    @Bean
    public static DataSource dataSource() {
        return DataSourceBuilder.create().url("jdbc:h2:mem:db;autocommit=off;").username("").password("").build();
    }
    
    @Bean
    @Primary
    public static DSLContext dslContext(final DataSource ds) {
        final ConnectionProvider connectionProvider = new DataSourceConnectionProvider(ds);
        final TransactionProvider transactionProvider = new ThreadLocalTransactionProvider(connectionProvider);
        final org.jooq.Configuration configuration = new DefaultConfiguration().set(SQLDialect.H2).set(transactionProvider);
        return DSL.using(configuration);
    }
    
    @Bean
    @Primary
    public static ClusterRegistry clusterRegistry() {
        return new LocalCluster(Collections.emptySet(), Collections.emptySet());
    }
    
    public static class UnitTestSchemaManager extends ExternalResource {
        
        private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUnitTest.class);
        
        private final Collection<Schema> schemas;
        private final DSLContext dslContext;
        
        public UnitTestSchemaManager(final Collection<Schema> schemas) {
            this.schemas = new LinkedList<>(schemas);
            this.dslContext = UnitTestConfiguration.dslContext(UnitTestConfiguration.dataSource());
        }
        
        private void createSchema(final DSLContext dsl, final Schema defaultSchema) {
            dsl.ddl(defaultSchema).queryStream().forEach(dsl::execute);
        }
        
        private void destroySchema(final DSLContext dsl, final Schema defaultSchema) {
            defaultSchema.getTables().forEach(table -> dsl.dropTable(table).execute());
        }
        
        @Override
        protected void before() throws Throwable {
            LOGGER.info("Creating schema");
            schemas.forEach(schema -> createSchema(dslContext, schema));
        }
        
        @Override
        protected void after() {
            LOGGER.info("Dropping schema");
            schemas.forEach(schema -> destroySchema(dslContext, schema));
        }
    }
    
}
