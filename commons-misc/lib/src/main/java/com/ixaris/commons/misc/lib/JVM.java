package com.ixaris.commons.misc.lib;

import com.ixaris.commons.misc.lib.logging.Logger;
import com.ixaris.commons.misc.lib.logging.LoggerFactory;
import java.lang.management.ManagementFactory;

public final class JVM {
    
    private static final Logger LOG = LoggerFactory.forEnclosingClass();
    private static final boolean DEBUG_MODE;
    
    static {
        boolean debugMode = false;
        try {
            final String inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
            debugMode = inputArgs.contains("-agentlib:jdwp") || inputArgs.contains("-Xrunjdwp");
        } catch (final RuntimeException e) {
            LOG.atError(e).log("Could not determine whether JVM is in debug mode. Assuming NO");
        }
        DEBUG_MODE = debugMode;
    }
    
    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }
    
    private JVM() {}
    
}
