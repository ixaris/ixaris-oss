package com.ixaris.commons.microservices.defaults.test.local;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.ixaris.commons.microservices.defaults.test.TestConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestApplication.class, TestConfiguration.class }, webEnvironment = DEFINED_PORT)
@TestPropertySource(properties = { "eventpublish.refreshinterval=200", "local=true", "cluster.enabled=false" })
public abstract class AbstractLocalWebStackIT {
    
    @BeforeClass
    public static void setEnvironmentVariables() {
        System.setProperty("server.port", "0"); // avoid port conflicts, allow things to run in parallel
        System.setProperty("spring.cloud.config.enabled", "false");
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
    }
    
    @ClassRule
    public static final ApplicationNameRule RULE_APPLICATION_NAME = new ApplicationNameRule();
    
    @Rule
    public final TestWatcher testNameRule = new TestNameLoggerWatcher();
    
    // see javadocs; but basically gives us better errors
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    
}
