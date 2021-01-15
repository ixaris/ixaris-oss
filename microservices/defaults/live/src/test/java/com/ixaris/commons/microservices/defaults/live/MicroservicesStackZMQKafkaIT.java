package com.ixaris.commons.microservices.defaults.live;

import static com.ixaris.common.zookeeper.test.TestZookeeperServer.TEST_ZK_PORT;
import static com.ixaris.commons.kafka.test.TestKafkaServer.TEST_KAFKA_HOST;
import static com.ixaris.commons.kafka.test.TestKafkaServer.TEST_KAFKA_PORT;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;

import com.ixaris.commons.microservices.defaults.live.support.StackTestTestStack;
import com.ixaris.commons.microservices.secrets.CertificateLoader;
import com.ixaris.commons.microservices.secrets.CertificateLoaderImpl;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

@DirtiesContext
@ContextConfiguration(classes = MicroservicesStackZMQKafkaIT.Config.class, initializers = MicroservicesStackZMQKafkaIT.Initializer.class)
public class MicroservicesStackZMQKafkaIT extends AbstractMicroservicesStackIT {
    
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        
        @Override
        public void initialize(final ConfigurableApplicationContext ctx) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx,
                "kafka.url=" + TEST_KAFKA_HOST + ":" + TEST_KAFKA_PORT,
                "zookeeper.url=127.0.0.1:" + TEST_ZK_PORT);
        }
        
    }
    
    @ImportResource("classpath*:spring/*.xml")
    public static class Config {
        
        @Bean
        public ApplicationListener<ContextRefreshedEvent> setTestTenants(final MultiTenancy multiTenancy) {
            return e -> multiTenancy.setTenants(Arrays.asList(MultiTenancy.SYSTEM_TENANT, TestTenants.DEFAULT, TestTenants.LEFT, TestTenants.RIGHT));
        }
        
        @Bean
        public static CertificateLoader certificateLoader(@Value("${environment.name}") final String environment,
                                                          @Value("${spring.application.name}") final String serviceName) {
            return new CertificateLoaderImpl(environment, serviceName, "../../../secrets");
        }
        
    }
    
    private static StackTestTestStack TEST_STACK;
    
    @BeforeClass
    public static void beforeClass() {
        TEST_STACK = new StackTestTestStack();
        TEST_STACK.start();
    }
    
    @AfterClass
    public static void afterClass() {
        TEST_STACK.stop();
        TEST_STACK = null;
    }
    
}
