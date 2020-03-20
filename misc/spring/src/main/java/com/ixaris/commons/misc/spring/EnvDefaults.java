package com.ixaris.commons.misc.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.Environment;

public final class EnvDefaults {
    
    private static final Log LOG = LogFactory.getLog(EnvDefaults.class);
    
    public static <T> T getOrDefault(
                                     final Environment env, final String name, final Class<T> type, final T defaultValue) {
        final T value = env.getProperty(name, type);
        if (value == null) {
            LOG.warn(name + " property is not defined. Defaulting to: " + defaultValue);
            return defaultValue;
        } else {
            return value;
        }
    }
    
    public static String getOrDefault(final Environment env, final String name, final String defaultValue) {
        return getOrDefault(env, name, String.class, defaultValue);
    }
    
    public static String getEnvironmentName(final Environment env) {
        final String[] activeProfiles = env.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return activeProfiles[activeProfiles.length - 1];
        } else {
            return "";
        }
    }
    
    private EnvDefaults() {}
    
}
