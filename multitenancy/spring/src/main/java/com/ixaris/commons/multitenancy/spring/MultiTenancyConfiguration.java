package com.ixaris.commons.multitenancy.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;

@Configuration
public class MultiTenancyConfiguration {
    
    @Bean(destroyMethod = "stop")
    public MultiTenancy multiTenancy() {
        final MultiTenancy multiTenancy = new MultiTenancy();
        multiTenancy.start();
        return multiTenancy;
    }
    
}
