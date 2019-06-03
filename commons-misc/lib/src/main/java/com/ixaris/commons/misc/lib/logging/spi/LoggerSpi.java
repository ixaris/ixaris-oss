package com.ixaris.commons.misc.lib.logging.spi;

import com.ixaris.commons.misc.lib.logging.Logger.Level;

public interface LoggerSpi {
    
    boolean isEnabled(Level level);
    
    /**
     * log at the requested level, the given cause (may be null) and the given message
     */
    void log(Level level, Throwable cause, String message);
    
    /**
     * log at the requested level, the given cause (may be null) and the given message with the given arguments
     */
    void log(Level level, Throwable cause, String message, Object... arguments);
    
}
