package com.ixaris.commons.microservices.defaults.app;

import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

/**
 * A Log4J2LoggingSystem which copies additional properties from the current environment to Java System properties and makes them accessible in
 * the Log4J2 configuration file.
 */
public class Log4J2System extends Log4J2LoggingSystem {
    
    private static final String GRAYLOG_HOST_ADDRESS = "GRAYLOG_HOST_ADDRESS";
    private static final String GRAYLOG_HOST_PORT = "GRAYLOG_HOST_PORT";
    private static final String APPLICATION_NAME = "spring.application.name";
    private static final String ACTIVE_PROFILES = "spring.profiles.active";
    
    public Log4J2System(final ClassLoader classLoader) {
        super(classLoader);
    }
    
    @Override
    protected void loadConfiguration(final LoggingInitializationContext initializationContext, final String location, final LogFile logFile) {
        applyProperties(initializationContext);
        super.loadConfiguration(initializationContext, location, logFile);
    }
    
    private void applyProperties(final LoggingInitializationContext initializationContext) {
        if (initializationContext != null) {
            final Environment env = initializationContext.getEnvironment();
            setSystemProperty(env, GRAYLOG_HOST_ADDRESS, GRAYLOG_HOST_ADDRESS);
            setSystemProperty(env, GRAYLOG_HOST_PORT, GRAYLOG_HOST_PORT);
            setSystemProperty(env, APPLICATION_NAME, APPLICATION_NAME);
            setSystemProperty(env, ACTIVE_PROFILES, ACTIVE_PROFILES);
        }
    }
    
    private void setSystemProperty(PropertyResolver resolver, String systemPropertyName, String propertyName) {
        final String value = resolver.getProperty(propertyName);
        if (System.getProperty(systemPropertyName) == null && value != null) {
            System.setProperty(systemPropertyName, value);
        }
    }
}
