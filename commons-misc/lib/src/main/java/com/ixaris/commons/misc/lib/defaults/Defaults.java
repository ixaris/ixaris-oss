package com.ixaris.commons.misc.lib.defaults;

import com.ixaris.commons.misc.lib.JVM;
import com.ixaris.commons.misc.lib.logging.Logger;
import com.ixaris.commons.misc.lib.logging.LoggerFactory;

public final class Defaults {
    
    private static final Logger LOG = LoggerFactory.forEnclosingClass();
    
    // should be used as the default timeout most of the time.
    // Will be set to 1 minute or 5 minutes depending on whether we are in debug mode
    public static final int DEFAULT_TIMEOUT = JVM.isDebugMode() ? 300_000 : 60_000;
    
    public static <T> T getOrDefault(final String name, final T prop, final T defaultValue) {
        if (prop == null) {
            LOG.atWarn().log(name + " property is not defined. Defaulting to: " + defaultValue);
            return defaultValue;
        } else {
            return prop;
        }
    }
    
    private Defaults() {}
    
}
