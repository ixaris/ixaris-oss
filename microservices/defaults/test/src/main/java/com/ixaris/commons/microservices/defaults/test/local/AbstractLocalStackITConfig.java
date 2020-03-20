package com.ixaris.commons.microservices.defaults.test.local;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.ixaris.commons.microservices.defaults.live.LiveConfiguration;
import com.ixaris.commons.microservices.defaults.test.SpringResources;
import com.ixaris.commons.microservices.defaults.test.TestConfiguration;

/**
 * @author <a href="keith.spiteri@ixaris.com">keith.spiteri</a>
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { SpringResources.class, LiveConfiguration.class, TestConfiguration.class })
@TestPropertySource(locations = "classpath:app.properties", properties = { "eventpublish.refreshinterval=200", "local=true", "cluster.enabled=false" })
public abstract class AbstractLocalStackITConfig {
    
    @ClassRule
    public static final ApplicationNameRule RULE_APPLICATION_NAME = new ApplicationNameRule();
    
    @Rule
    public final TestWatcher testNameRule = new TestNameLoggerWatcher();
    
    // see javadocs; but basically gives us better errors
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    
}
