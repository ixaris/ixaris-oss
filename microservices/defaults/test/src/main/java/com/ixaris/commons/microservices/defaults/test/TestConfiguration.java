package com.ixaris.commons.microservices.defaults.test;

import com.ixaris.commons.jooq.test.MultiTenancyTestDataSourceConfig;
import com.ixaris.commons.microservices.secrets.CertificateLoader;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;
import java.util.Arrays;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;

@Configuration
@Import(MultiTenancyTestDataSourceConfig.class)
@SuppressWarnings("squid:S1118")
public class TestConfiguration {
    
    static {
        System.setProperty("authentication.signing.algorithm", "SHA256withRSA");
    }
    
    @Bean
    public static ApplicationListener<ContextRefreshedEvent> installTestTenants(final MultiTenancy multiTenancy) {
        return e -> multiTenancy.setTenants(Arrays.asList(
            MultiTenancy.SYSTEM_TENANT, TestTenants.DEFAULT, TestTenants.LEFT, TestTenants.RIGHT));
    }
    
    @Bean
    @Primary
    public static CertificateLoader certificateLoader() {
        return new TestCertificateLoader();
    }
    
}
