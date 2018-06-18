package com.ixaris.commons.misc.lib;

import java.lang.management.ManagementFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JVM {
    
    private static final Logger LOG = LoggerFactory.getLogger(JVM.class);
    private static final boolean DEBUG_MODE;
    
    static {
        boolean debugMode = false;
        try {
            final String inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
            debugMode = inputArgs.contains("-agentlib:jdwp") || inputArgs.contains("-Xrunjdwp");
        } catch (final RuntimeException e) {
            LOG.error("Could not determine whether JVM is in debug mode. Assuming NO", e);
        }
        DEBUG_MODE = debugMode;
    }
    
    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }
    
    private JVM() {}
    
}
