package com.ixaris.commons.spring.health;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.ReflectionUtils;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

/**
 * Configuration to allow Spring to make use of health checks registered in Dropwizard/CodeHale's {@link HealthCheckRegistry}. Limitation: can
 * only pick on health checks that are created during Spring context creation, nothing afterwards. This is because it creates an {@link
 * ApplicationListener} to watch for {@link ContextRefreshedEvent}s to be able to perform its processing.
 *
 * <p>Why?
 *
 * <p>Spring Boot Actuator has a {@link HealthEndpoint} as one of its <a
 * href="http://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html">endpoints that it automatically
 * creates</a>. The endpoint works on {@link org.springframework.boot.actuate.health.HealthIndicator}s.
 *
 * <p>Dropwizard's {@link HealthCheckRegistry} offers very similar functionality to Spring Boot Actuators health indicators, but is not tied to
 * Spring in any way. The {@link com.zaxxer.hikari.HikariConfig} is at least one instance of an external library making use of Dropwizard's
 * health checks, so it makes sense to integrate the two technologies. Unfortunately, the integration isn't very 'clean', as both techs hide away
 * the details beneath private final variables, so some forceful reflection had to be used - but the end result is Dropwizard health checks
 * working with Spring Boot Actuator (and all tools based on it).
 *
 * @author benjie.gatt
 */
@Configuration
public class DropwizardHealthCheckSpringBridgeConfig {
    
    private static final Logger LOG = LoggerFactory.getLogger(DropwizardHealthCheckSpringBridgeConfig.class);
    
    @Bean
    public static HealthCheckRegistry healthCheckRegistry() {
        return new HealthCheckRegistry();
    }
    
    @Bean
    @SuppressWarnings("squid:S1698")
    public static ApplicationListener<ContextRefreshedEvent> healthCheckProcessor(final HealthCheckRegistry healthCheckRegistry,
                                                                                  final Optional<HealthEndpoint> healthEndpoint,
                                                                                  final ConfigurableListableBeanFactory beanFactory,
                                                                                  final ApplicationContext context) {
        
        return event -> {
            // Make sure that the event is related to the "main" application context. E.g. Spring cloud bus
            // brings in another application context which would cause duplicate health check registration.
            if (context == event.getSource() && healthEndpoint.isPresent()) {
                final ConcurrentMap<String, HealthCheck> healthChecks = getHealthChecks(healthCheckRegistry);
                for (Map.Entry<String, HealthCheck> healthCheck : healthChecks.entrySet()) {
                    LOG.info("Registering health check [{}]", healthCheck.getKey());
                    final DropwizardHealthCheckBridge bridge = new DropwizardHealthCheckBridge(healthCheck.getValue());
                    // we technically don't *need* to do this because we are forcefully including the checks into the
                    // endpoint below,
                    // but normally health indicators are registered as beans, so there is no reason to break this
                    // assumption...
                    beanFactory.registerSingleton(healthCheck.getKey(), bridge);
                    
                    final CompositeHealthIndicator healthIndicator = getCompositeHealthIndicator(healthEndpoint.get());
                    healthIndicator.getRegistry().register(healthCheck.getKey(), healthIndicator);
                }
            } else if (context == event.getSource()) {
                LOG
                    .warn("Health endpoint not found - assuming Spring Boot Actuator is disabled and ignoring registration of CodeHale healthchecks. "
                        + "This will not impact monitoring as there is no endpoint to query.");
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    private static CompositeHealthIndicator getCompositeHealthIndicator(final HealthEndpoint healthEndpoint) {
        final Field field = ReflectionUtils.findField(HealthEndpoint.class, "healthIndicator");
        field.setAccessible(true);
        final CompositeHealthIndicator healthIndicator = (CompositeHealthIndicator) ReflectionUtils.getField(field, healthEndpoint);
        return healthIndicator;
    }
    
    @SuppressWarnings("unchecked")
    private static ConcurrentMap<String, HealthCheck> getHealthChecks(final HealthCheckRegistry healthCheckRegistry) {
        final Field field = ReflectionUtils.findField(HealthCheckRegistry.class, "healthChecks");
        field.setAccessible(true);
        final ConcurrentMap<String, HealthCheck> healthChecks = (ConcurrentMap<String, HealthCheck>) ReflectionUtils.getField(field, healthCheckRegistry);
        return healthChecks;
    }
    
}
