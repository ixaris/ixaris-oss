package com.ixaris.commons.microservices.defaults.test;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;
import java.util.Arrays;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

@Configuration
@SuppressWarnings("squid:S1118")
public class TestConfiguration {
    
    @Bean
    public static ApplicationListener<ContextRefreshedEvent> installTestTenants(final MultiTenancy multiTenancy) {
        return e -> multiTenancy.setTenants(Arrays.asList(
            MultiTenancy.SYSTEM_TENANT, TestTenants.DEFAULT, TestTenants.LEFT, TestTenants.RIGHT));
    }
    
}
