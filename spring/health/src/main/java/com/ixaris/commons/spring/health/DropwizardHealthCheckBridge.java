package com.ixaris.commons.spring.health;

import java.util.Objects;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import com.codahale.metrics.health.HealthCheck;

/**
 * Simple bridge between Dropwizard {@link HealthCheck} and Spring Boot {@link HealthIndicator}. This allows other tools based on Spring boot to
 * make use of the health checks.
 *
 * @author benjie.gatt
 */
public class DropwizardHealthCheckBridge implements HealthIndicator {
    
    private final HealthCheck healthCheck;
    
    public DropwizardHealthCheckBridge(final HealthCheck healthCheck) {
        Objects.requireNonNull(healthCheck);
        this.healthCheck = healthCheck;
    }
    
    @Override
    public Health health() {
        final HealthCheck.Result result = healthCheck.execute();
        final Health.Builder health = new Health.Builder();
        if (result.isHealthy()) {
            health.up();
        } else {
            if (result.getError() instanceof Exception) {
                health.down((Exception) result.getError());
            } else {
                health.down();
            }
        }
        
        if (result.getMessage() != null) {
            health.withDetail("message", result.getMessage());
        }
        
        return health.build();
    }
}
