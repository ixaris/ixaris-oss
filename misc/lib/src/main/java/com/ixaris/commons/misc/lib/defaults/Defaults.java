package com.ixaris.commons.misc.lib.defaults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.misc.lib.JVM;

public final class Defaults {
    
    private static final Logger LOG = LoggerFactory.getLogger(Defaults.class);
    
    // should be used as the default timeout most of the time.
    // Will be set to 1 minute or 5 minutes depending on whether we are in debug mode
    public static final int DEFAULT_TIMEOUT = JVM.isDebugMode() ? 300_000 : 60_000;
    
    public static <T> T getOrDefault(final String name, final T prop, final T defaultValue) {
        if (prop == null) {
            LOG.warn(name + " property is not defined. Defaulting to: " + defaultValue);
            return defaultValue;
        }
        return prop;
    }
    
    private Defaults() {}
    
}
