package com.ixaris.commons.microservices.defaults.test.local;

import org.junit.BeforeClass;

public abstract class AbstractLocalStackIT extends AbstractLocalStackITConfig {
    
    @BeforeClass
    public static void setEnvironmentVariables() {
        System.setProperty("spring.cloud.config.enabled", "false");
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
    }
    
}
