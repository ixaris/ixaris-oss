package com.ixaris.commons.misc.lib.logging;

import com.ixaris.commons.misc.lib.logging.spi.LoggerSpi;
import java.util.Arrays;
import java.util.function.Consumer;

@SuppressWarnings("squid:ClassCyclomaticComplexity")
public final class Logger {
    
    public enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
    
    private static final NoOp NO_OP = new NoOp();
    
    private final LoggerSpi loggerImpl;
    
    Logger(final LoggerSpi loggerImpl) {
        this.loggerImpl = loggerImpl;
    }
    
    public LoggerContext atTrace() {
        return at(Level.TRACE, null);
    }
    
    public LoggerContext atTrace(final Throwable cause) {
        return at(Level.TRACE, cause);
    }
    
    public LoggerContext atDebug() {
        return at(Level.DEBUG, null);
    }
    
    public LoggerContext atDebug(final Throwable cause) {
        return at(Level.DEBUG, cause);
    }
    
    public LoggerContext atInfo() {
        return at(Level.INFO, null);
    }
    
    public LoggerContext atInfo(final Throwable cause) {
        return at(Level.INFO, cause);
    }
    
    public LoggerContext atWarn() {
        return at(Level.WARN, null);
    }
    
    public LoggerContext atWarn(final Throwable cause) {
        return at(Level.WARN, cause);
    }
    
    public LoggerContext atError() {
        return at(Level.ERROR, null);
    }
    
    public LoggerContext atError(final Throwable cause) {
        return at(Level.ERROR, cause);
    }
    
    public LoggerContext at(final Level level) {
        return at(level, null);
    }
    
    public LoggerContext at(final Level level, final Throwable cause) {
        return loggerImpl.isEnabled(level) ? new Context(level, cause) : NO_OP;
    }
    
    @SuppressWarnings({ "squid:S2972", "squid:S1448", "squid:ClassCyclomaticComplexity" })
    final class Context implements LoggerContext {
        
        private final Level level;
        private final Throwable cause;
        
        private Context(Level level, final Throwable cause) {
            this.level = level;
            this.cause = cause;
        }
        
        @Override
        public void ifEnabled(final Consumer<LoggerContext> consumer) {
            consumer.accept(this);
        }
        
        /**
         * Make the backend logging call. This is the point at which we have paid the price of creating a varargs array
         * and doing any necessary auto-boxing.
         */
        @SuppressWarnings("ReferenceEquality")
        private void logImpl(final String message, final Object... args) {
            if ((args == null) || args.length == 0) {
                loggerImpl.log(level, cause, message);
            } else {
                loggerImpl.log(level, cause, message, args);
            }
        }
        
        @Override
        public final void log() {
            logImpl("");
        }
        
        @Override
        public void logVarargs(final String message, final Object[] varargs) {
            logImpl(message, Arrays.copyOf(varargs, varargs.length));
        }
        
        @Override
        public final void log(final String msg) {
            logImpl(msg);
        }
        
        @Override
        public final void log(final String message, final Object p1) {
            logImpl(message, p1);
        }
        
        @Override
        public final void log(final String message, final Object p1, final Object p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final Object p1, final Object p2, final Object p3) {
            logImpl(message, p1, p2, p3);
        }
        
        @Override
        public final void log(
            final String message, final Object p1, final Object p2, final Object p3, final Object p4
        ) {
            logImpl(message, p1, p2, p3, p4);
        }
        
        @Override
        public final void log(
            final String msg, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5
        ) {
            logImpl(msg, p1, p2, p3, p4, p5);
        }
        
        @Override
        public final void log(
            final String msg,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6
        ) {
            logImpl(msg, p1, p2, p3, p4, p5, p6);
        }
        
        @Override
        public final void log(
            final String msg,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7
        ) {
            logImpl(msg, p1, p2, p3, p4, p5, p6, p7);
        }
        
        @Override
        public final void log(
            final String msg,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8
        ) {
            logImpl(msg, p1, p2, p3, p4, p5, p6, p7, p8);
        }
        
        @Override
        public final void log(
            final String msg,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9
        ) {
            logImpl(msg, p1, p2, p3, p4, p5, p6, p7, p8, p9);
        }
        
        @Override
        public final void log(
            final String msg,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9,
            final Object p10
        ) {
            logImpl(msg, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
        }
        
        @Override
        public final void log(
            final String msg,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9,
            final Object p10,
            final Object... rest
        ) {
            // Manually create a new varargs array and copy the parameters in.
            final Object[] params = new Object[rest.length + 10];
            params[0] = p1;
            params[1] = p2;
            params[2] = p3;
            params[3] = p4;
            params[4] = p5;
            params[5] = p6;
            params[6] = p7;
            params[7] = p8;
            params[8] = p9;
            params[9] = p10;
            System.arraycopy(rest, 0, params, 10, rest.length);
            logImpl(msg, params);
        }
        
        @Override
        public final void log(final String message, final char p1) {
            logImpl(message, p1);
        }
        
        @Override
        public final void log(final String message, final byte p1) {
            logImpl(message, p1);
        }
        
        @Override
        public final void log(final String message, final short p1) {
            logImpl(message, p1);
        }
        
        @Override
        public final void log(final String message, final int p1) {
            logImpl(message, p1);
        }
        
        @Override
        public final void log(final String message, final long p1) {
            logImpl(message, p1);
        }
        
        @Override
        public final void log(final String message, final Object p1, final boolean p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final Object p1, final char p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final Object p1, final byte p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final Object p1, final short p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final Object p1, final int p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final Object p1, final long p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final Object p1, final float p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final Object p1, final double p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final boolean p1, final Object p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final char p1, final Object p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final byte p1, final Object p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final short p1, final Object p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final int p1, final Object p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final long p1, final Object p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final float p1, final Object p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final double p1, final Object p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final boolean p1, final boolean p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final char p1, final boolean p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final byte p1, final boolean p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final short p1, final boolean p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final int p1, final boolean p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final long p1, final boolean p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final float p1, final boolean p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final double p1, final boolean p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final boolean p1, final char p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final char p1, final char p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final byte p1, final char p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final short p1, final char p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final int p1, final char p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final long p1, final char p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final float p1, final char p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final double p1, final char p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final boolean p1, final byte p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final char p1, final byte p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final byte p1, final byte p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final short p1, final byte p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final int p1, final byte p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final long p1, final byte p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final float p1, final byte p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final double p1, final byte p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final boolean p1, final short p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final char p1, final short p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final byte p1, final short p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final short p1, final short p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final int p1, final short p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final long p1, final short p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final float p1, final short p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final double p1, final short p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final boolean p1, final int p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final char p1, final int p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final byte p1, final int p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final short p1, final int p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final int p1, final int p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final long p1, final int p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final float p1, final int p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final double p1, final int p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final boolean p1, final long p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final char p1, final long p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final byte p1, final long p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final short p1, final long p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final int p1, final long p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final long p1, final long p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final float p1, final long p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final double p1, final long p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final boolean p1, final float p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final char p1, final float p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final byte p1, final float p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final short p1, final float p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final int p1, final float p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final long p1, final float p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final float p1, final float p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final double p1, final float p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final boolean p1, final double p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final char p1, final double p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final byte p1, final double p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final short p1, final double p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final int p1, final double p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final long p1, final double p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final float p1, final double p2) {
            logImpl(message, p1, p2);
        }
        
        @Override
        public final void log(final String message, final double p1, final double p2) {
            logImpl(message, p1, p2);
        }
    }
    
}
