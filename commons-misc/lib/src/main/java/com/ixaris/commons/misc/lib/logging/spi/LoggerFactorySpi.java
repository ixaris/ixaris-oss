package com.ixaris.commons.misc.lib.logging.spi;

@FunctionalInterface
public interface LoggerFactorySpi {
    
    LoggerSpi getLogger(Class<?> cls);
    
}
