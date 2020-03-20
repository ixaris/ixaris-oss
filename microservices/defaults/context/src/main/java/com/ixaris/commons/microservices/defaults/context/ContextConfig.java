package com.ixaris.commons.microservices.defaults.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;

@Configuration
public class ContextConfig {
    
    @Bean
    public static ServiceSecurityChecker contextServiceSecurityChecker() {
        return new ContextServiceSecurityChecker();
    }
    
    @Bean
    public static ServiceAsyncInterceptor contextServiceAsyncInterceptor() {
        return new ContextServiceAsyncInterceptor();
    }
    
}
