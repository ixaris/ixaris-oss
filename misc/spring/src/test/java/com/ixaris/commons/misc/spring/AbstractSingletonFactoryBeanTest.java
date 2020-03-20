package com.ixaris.commons.misc.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.ixaris.commons.misc.spring.singleton.Counter;
import com.ixaris.commons.misc.spring.singleton.TestSingleton;
import com.ixaris.commons.misc.spring.singleton.TestTypeRegistry;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class AbstractSingletonFactoryBeanTest {
    
    @Configuration
    @ComponentScan("com.ixaris.commons.misc.spring")
    public static class TestConfiguration {}
    
    @Autowired
    private TestSingleton testSingleton;
    
    @Autowired
    private TestTypeRegistry testTypeRegistry;
    
    @Autowired
    private Counter counter;
    
    @Test
    public void simpleTest() {
        assertTrue(testSingleton == TestSingleton.getInstance()); // check reference equality
        assertEquals(1, testTypeRegistry.getRegisteredKeys().size());
        assertEquals(1, counter.getCounter());
    }
    
}
