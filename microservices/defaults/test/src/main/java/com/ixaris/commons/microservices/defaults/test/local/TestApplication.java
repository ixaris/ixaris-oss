package com.ixaris.commons.microservices.defaults.test.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import com.ixaris.commons.microservices.defaults.live.LiveConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, JooqAutoConfiguration.class, FlywayAutoConfiguration.class })
@EnableConfigurationProperties
@PropertySource("classpath:app.properties")
@Import({ LiveConfiguration.class })
public class TestApplication {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TestApplication.class);
    
    private static final int STATUS_CODE = 200;
    
    @SuppressWarnings("squid:S2095")
    // this is the main class entrypoint
    public static void main(final String... args) {
        try {
            final ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .registerShutdownHook(false)
                .bannerMode(Mode.OFF)
                .sources(TestApplication.class)
                .build()
                .run(args);
            // note: see log4j xml configs - we have suppressed the shutdown there and are intentionally not shutting it
            // down here to be able to actually view shutdown logs!
            Runtime.getRuntime().addShutdownHook(new Thread(context::close));
        } catch (Throwable t) { // NOSONAR in this case it is needed to catch any Throwable, even Errors
            // avoiding system hanging if there is any error
            LOGGER.error("\tExiting due to exception during application initialization.", t);
            LOGGER.info("Exiting microservice now.");
            System.exit(STATUS_CODE);
        }
    }
    
}
