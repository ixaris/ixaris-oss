package com.ixaris.commons.microservices.defaults.test.initializer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.ixaris.commons.microservices.defaults.test.TestStack;

/**
 * @author benjie.gatt
 */
@Component
public class StackInitializer {
    
    private static final TestStack STACK = new TestStack();
    
    @PostConstruct
    public void setupStack() {
        STACK.start();
    }
    
    @PreDestroy
    public void teardownStack() {
        STACK.stop();
    }
}
