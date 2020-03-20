package com.ixaris.commons.microservices.defaults.test.local;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author <a href="keith.spiteri@ixaris.com">keith.spiteri</a>
 */
public class ApplicationNameRule implements TestRule {
    
    @Override
    public Statement apply(final Statement base, final Description description) {
        System.setProperty("environment.name", "test");
        final CustomApplicationName appNameAnnotation = description.getTestClass().getAnnotation(CustomApplicationName.class);
        if (appNameAnnotation == null || appNameAnnotation.value().isEmpty()) {
            String name = description.getTestClass().getSimpleName();
            if (name.length() > 30) {
                name = name.substring(0, 30);
            }
            System.setProperty("spring.application.name", name);
        } else {
            System.setProperty("spring.application.name", appNameAnnotation.value());
        }
        return base;
    }
}
