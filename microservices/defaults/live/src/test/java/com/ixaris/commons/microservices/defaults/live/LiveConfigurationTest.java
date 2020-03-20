package com.ixaris.commons.microservices.defaults.live;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

public class LiveConfigurationTest {
    
    private AnnotationConfigApplicationContext context = null;
    
    @After
    public void after() {
        if (context != null) {
            context.close();
            context = null;
        }
    }
    
    @Test
    public void itShouldDefaultZkUrls() {
        load(TestConfiguration.class, "zookeeper.url: 10.0.0.1");
        final Environment env = context.getBean(Environment.class);
        Assert.assertEquals("zookeeper.url should default to 10.0.0.1", "10.0.0.1", env.getProperty("zookeeper.url"));
        context.close();
    }
    
    @Test
    public void itShouldOverrideDefaultKafkaTopicPrefixWithACustomValue() {
        load(TestConfiguration.class, "multitenancy.kafka.topic-prefix: new-prefix");
        final Environment env = context.getBean(Environment.class);
        Assert.assertEquals("multitenancy.kafka.topic-prefix should be new-prefix",
            "new-prefix",
            env.getProperty("multitenancy.kafka.topic-prefix"));
        context.close();
    }
    
    private void load(final Class<?> config, final String... env) {
        context = new AnnotationConfigApplicationContext();
        EnvironmentTestUtils.addEnvironment(context, env);
        context.register(config);
        context.refresh();
    }
    
    // Importing LiveConfiguration here would be too heavy-weight.
    @Configuration
    static class TestConfiguration {}
}
