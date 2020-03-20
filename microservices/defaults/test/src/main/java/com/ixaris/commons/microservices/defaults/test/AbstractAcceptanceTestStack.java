package com.ixaris.commons.microservices.defaults.test;

import org.junit.ClassRule;
import org.junit.rules.TestRule;

/**
 * @author <a href="mailto:kurt.micallef@ixaris.com">Kurt Micallef</a>
 */
public abstract class AbstractAcceptanceTestStack { // NOSONAR: This is a test class
    
    @ClassRule
    public static final TestRule RULE_APPLICATION_NAME = (base, description) -> {
        System.setProperty("environment.name", "test");
        System.setProperty("local", "true");
        System.setProperty("spring.application.name", description.getTestClass().getSimpleName());
        
        // do not use service config. use tests configuration files/classes instead
        System.setProperty("spring.cloud.config.enabled", "false");
        return base;
    };
}
