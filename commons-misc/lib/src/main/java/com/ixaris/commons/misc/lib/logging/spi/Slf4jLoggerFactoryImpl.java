package com.ixaris.commons.misc.lib.logging.spi;

import com.ixaris.commons.misc.lib.logging.Logger.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Slf4jLoggerFactoryImpl implements LoggerFactorySpi {
    
    public static final Slf4jLoggerFactoryImpl INSTANCE = new Slf4jLoggerFactoryImpl();
    
    private Slf4jLoggerFactoryImpl() {}
    
    @Override
    public LoggerSpi getLogger(final Class<?> cls) {
        return new Slf4jLoggerImpl(LoggerFactory.getLogger(cls));
    }
    
    @SuppressWarnings("squid:S2972")
    private static class Slf4jLoggerImpl implements LoggerSpi {
        
        private final Logger logger;
        
        private Slf4jLoggerImpl(final Logger logger) {
            this.logger = logger;
        }
        
        @Override
        public boolean isEnabled(final Level level) {
            switch (level) {
                case TRACE:
                    return logger.isTraceEnabled();
                case DEBUG:
                    return logger.isDebugEnabled();
                case INFO:
                    return logger.isInfoEnabled();
                case WARN:
                    return logger.isWarnEnabled();
                case ERROR:
                    return logger.isErrorEnabled();
                default:
                    return false;
            }
        }
        
        @SuppressWarnings("squid:MethodCyclomaticComplexity")
        @Override
        public void log(final Level level, final Throwable cause, final String message) {
            switch (level) {
                case TRACE:
                    if (cause != null) {
                        logger.trace(message, cause);
                    } else {
                        logger.trace(message);
                    }
                    break;
                case DEBUG:
                    if (cause != null) {
                        logger.debug(message, cause);
                    } else {
                        logger.debug(message);
                    }
                    break;
                case INFO:
                    if (cause != null) {
                        logger.info(message, cause);
                    } else {
                        logger.info(message);
                    }
                    break;
                case WARN:
                    if (cause != null) {
                        logger.warn(message, cause);
                    } else {
                        logger.warn(message);
                    }
                    break;
                case ERROR:
                    if (cause != null) {
                        logger.error(message, cause);
                    } else {
                        logger.error(message);
                    }
                    break;
                default:
            }
        }
        
        @SuppressWarnings("squid:MethodCyclomaticComplexity")
        @Override
        public void log(final Level level, final Throwable cause, final String message, final Object... arguments) {
            switch (level) {
                case TRACE:
                    if (cause != null) {
                        if (arguments.length == 0) {
                            logger.trace(message, cause);
                        } else {
                            logger.trace(message, appendThrowableToArguments(arguments, cause));
                        }
                    } else {
                        logger.trace(message, arguments);
                    }
                    break;
                case DEBUG:
                    if (cause != null) {
                        if (arguments.length == 0) {
                            logger.debug(message, cause);
                        } else {
                            logger.debug(message, appendThrowableToArguments(arguments, cause));
                        }
                    } else {
                        logger.debug(message, arguments);
                    }
                    break;
                case INFO:
                    if (cause != null) {
                        if (arguments.length == 0) {
                            logger.info(message, cause);
                        } else {
                            logger.info(message, appendThrowableToArguments(arguments, cause));
                        }
                    } else {
                        logger.info(message, arguments);
                    }
                    break;
                case WARN:
                    if (cause != null) {
                        if (arguments.length == 0) {
                            logger.warn(message, cause);
                        } else {
                            logger.warn(message, appendThrowableToArguments(arguments, cause));
                        }
                    } else {
                        logger.warn(message, arguments);
                    }
                    break;
                case ERROR:
                    if (cause != null) {
                        if (arguments.length == 0) {
                            logger.error(message, cause);
                        } else {
                            logger.error(message, appendThrowableToArguments(arguments, cause));
                        }
                    } else {
                        logger.error(message, arguments);
                    }
                    break;
                default:
            }
        }
        
        private Object[] appendThrowableToArguments(final Object[] arguments, final Throwable cause) {
            final int length = arguments.length;
            final Object[] argsWithCause = new Object[length + 1];
            System.arraycopy(arguments, 0, argsWithCause, 0, length);
            argsWithCause[length] = cause;
            return argsWithCause;
        }
        
    }
    
}
