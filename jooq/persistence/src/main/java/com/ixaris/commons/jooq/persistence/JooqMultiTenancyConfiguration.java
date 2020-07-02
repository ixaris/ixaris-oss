package com.ixaris.commons.jooq.persistence;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration that sets up <a href="http://www.jooq.org/doc/latest/manual-single-page/">Jooq</a> for use with multi-tenant datasources. Mainly
 * required due to our connection pools being setup per data-source, rather than per tenant (thus requiring switching of schema depending on the
 * tenant using the data source).
 *
 * @author benjie.gatt
 */
@Configuration
public class JooqMultiTenancyConfiguration {
    
    public static DSLContext createDslContext(final DataSource dataSource) {
        // DSL context takes connections from datasource, which itself provides connections with the appropriate tenant
        // We need to make sure the SQL generated is not generated with the database name, so that we can 'dynamically'
        // use the datasource connections
        // note: JDBC connections must not have a preparedStatement cache
        // We disable the fetch warnings setting so that we optimise the number of statements we send to mysql for
        // performance improvement
        final Settings settings = new Settings().withRenderSchema(false).withFetchWarnings(false);
        return new DefaultDSLContext(dataSource, SQLDialect.MYSQL, settings);
    }
    
    @Bean
    public DSLContext dslContext(final DataSource dataSource) {
        return createDslContext(dataSource);
    }
    
    @Bean(destroyMethod = "shutdown")
    public JooqAsyncPersistenceProvider jooqAsyncPersistenceProvider(final DSLContext dslContext) {
        return new JooqAsyncPersistenceProvider(dslContext);
    }
    
}
