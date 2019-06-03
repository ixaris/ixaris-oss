package com.ixaris.commons.misc.lib.logging;

import java.util.function.Consumer;

@SuppressWarnings("squid:S1448")
public interface LoggerContext {
    
    /**
     * executes the consumer only if logging at the requested level is enabled. Typically used if gathering logging
     * information is expensive
     */
    void ifEnabled(Consumer<LoggerContext> consumer);
    
    /**
     * Terminal log statement when a message is not required. A {@code log} method must terminate all fluent logging
     * chains and the no-argument method can be used if there is no need for a log message. For example:
     *
     * <pre>{@code
     * logger.at(INFO).withCause(error).log();
     * }</pre>
     *
     * <p>However as it is good practice to give all log statements a meaningful log message, use of this method should
     * be rare.
     */
    void log();
    
    /**
     * Logs a formatted representation of values in the given array, using the specified message template.
     *
     * <p>This method is only expected to be invoked with an existing varargs array passed in from another method.
     * Unlike {@link #log(String, Object)}, which would treat an array as a single parameter, this method will unwrap
     * the given array.
     *
     * @param message the message template string containing a single argument placeholder.
     * @param varargs the non-null array of arguments to be formatted.
     */
    void logVarargs(String message, Object[] varargs);
    
    /**
     * Logs the given literal string without without interpreting any argument placeholders.
     *
     * <p>Important: This is intended only for use with hard-coded, literal strings which cannot contain user data. If
     * you wish to log user generated data, you should do something like:
     *
     * <pre>{@code
     * log("user data=%s", value);
     * }</pre>
     *
     * This serves to give the user data context in the log file but, more importantly, makes it clear which arguments
     * may contain PII and other sensitive data (which might need to be scrubbed during logging). This recommendation
     * also applies to all the overloaded {@code log()} methods below.
     */
    void log(String msg);
    
    // ---- Overloads for object arguments (to avoid vararg array creation). ----
    
    /**
     * Logs a formatted representation of the given parameter, using the specified message template. The message string
     * is expected to contain argument placeholder terms appropriate to the logger's choice of parser.
     *
     * <p>Note that printf-style loggers are always expected to accept the standard Java printf formatting characters
     * (e.g. "%s", "%d" etc...) and all flags unless otherwise stated. Null arguments are formatted as the literal
     * string {@code "null"} regardless of formatting flags.
     *
     * @param msg the message template string containing a single argument placeholder.
     */
    void log(String msg, Object p1);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, Object p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, Object p2, Object p3);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, Object p2, Object p3, Object p4);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, Object p2, Object p3, Object p4, Object p5);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(
        String msg, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9
    );
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(
        String msg,
        Object p1,
        Object p2,
        Object p3,
        Object p4,
        Object p5,
        Object p6,
        Object p7,
        Object p8,
        Object p9,
        Object p10
    );
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(
        String msg,
        Object p1,
        Object p2,
        Object p3,
        Object p4,
        Object p5,
        Object p6,
        Object p7,
        Object p8,
        Object p9,
        Object p10,
        Object... rest
    );
    
    // ---- Overloads for a single argument (to avoid auto-boxing and vararg array creation). ----
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, char p1);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, byte p1);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, short p1);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, int p1);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, long p1);
    
    // ---- Overloads for two arguments (to avoid auto-boxing and vararg array creation). ----
    /*
     * It may not be obvious why we need _all_ combinations of fundamental types here (because some
     * combinations should be rare enough that we can ignore them). However due to the precedence in
     * the Java compiler for converting fundamental types in preference to auto-boxing, and the need
     * to preserve information about the original type (byte, short, char etc...) when doing unsigned
     * formatting, it turns out that all combinations are required.
     */
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, boolean p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, char p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, byte p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, short p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, int p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, long p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, float p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, Object p1, double p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, boolean p1, Object p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, char p1, Object p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, byte p1, Object p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, short p1, Object p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, int p1, Object p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, long p1, Object p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, float p1, Object p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, double p1, Object p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, boolean p1, boolean p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, char p1, boolean p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, byte p1, boolean p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, short p1, boolean p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, int p1, boolean p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, long p1, boolean p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, float p1, boolean p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, double p1, boolean p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, boolean p1, char p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, char p1, char p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, byte p1, char p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, short p1, char p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, int p1, char p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, long p1, char p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, float p1, char p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, double p1, char p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, boolean p1, byte p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, char p1, byte p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, byte p1, byte p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, short p1, byte p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, int p1, byte p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, long p1, byte p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, float p1, byte p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, double p1, byte p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, boolean p1, short p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, char p1, short p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, byte p1, short p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, short p1, short p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, int p1, short p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, long p1, short p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, float p1, short p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, double p1, short p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, boolean p1, int p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, char p1, int p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, byte p1, int p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, short p1, int p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, int p1, int p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, long p1, int p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, float p1, int p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, double p1, int p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, boolean p1, long p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, char p1, long p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, byte p1, long p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, short p1, long p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, int p1, long p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, long p1, long p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, float p1, long p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, double p1, long p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, boolean p1, float p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, char p1, float p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, byte p1, float p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, short p1, float p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, int p1, float p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, long p1, float p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, float p1, float p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, double p1, float p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, boolean p1, double p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, char p1, double p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, byte p1, double p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, short p1, double p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, int p1, double p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, long p1, double p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, float p1, double p2);
    
    /**
     * Logs a message with formatted arguments (see {@link #log(String, Object)} for details).
     */
    void log(String msg, double p1, double p2);
    
}
