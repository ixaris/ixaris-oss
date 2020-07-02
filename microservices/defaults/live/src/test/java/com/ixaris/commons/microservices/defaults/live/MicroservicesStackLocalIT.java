package com.ixaris.commons.microservices.defaults.live;

import java.util.Arrays;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.ixaris.commons.microservices.defaults.live.MicroservicesStackLocalIT.LocalMicroservicesTestConfig;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

@DirtiesContext
@ContextConfiguration(classes = LocalMicroservicesTestConfig.class)
@TestPropertySource(properties = { "local=true", "akka.cluster.auto-join=false" })
@SuppressWarnings("squid:S1607")
public class MicroservicesStackLocalIT extends AbstractMicroservicesStackIT {
    
    @ImportResource("classpath*:spring/*.xml")
    public static class LocalMicroservicesTestConfig {
        
        @Bean
        public ApplicationListener<ContextRefreshedEvent> setTestTenants(final MultiTenancy multiTenancy) {
            return e -> multiTenancy.setTenants(Arrays.asList(MultiTenancy.SYSTEM_TENANT, TestTenants.DEFAULT, TestTenants.LEFT, TestTenants.RIGHT));
        }
        
    }
    
}
