package com.ixaris.commons.misc.lib.logging;

import java.util.function.Consumer;

/**
 * An implementation of {@link LoggerContext} which does nothing and discards all parameters.
 *
 * <p>This class (or a subclass in the case of an extended API) should be returned whenever logging is definitely
 * disabled (e.g. when the log level is too low).
 */
@SuppressWarnings({ "squid:S1186", "squid:S1448" })
class NoOp implements LoggerContext {
    
    @Override
    public final void ifEnabled(final Consumer<LoggerContext> consumer) {}
    
    @Override
    public final void log() {}
    
    @Override
    public void logVarargs(final String message, final Object[] varargs) {}
    
    @Override
    public final void log(final String msg) {}
    
    @Override
    public final void log(final String msg, final Object p1) {}
    
    @Override
    public final void log(final String msg, final Object p1, final Object p2) {}
    
    @Override
    public final void log(final String msg, final Object p1, final Object p2, final Object p3) {}
    
    @Override
    public final void log(final String msg, final Object p1, final Object p2, final Object p3, final Object p4) {}
    
    @Override
    public final void log(
        final String msg, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5
    ) {}
    
    @Override
    public final void log(
        final String msg,
        final Object p1,
        final Object p2,
        final Object p3,
        final Object p4,
        final Object p5,
        final Object p6
    ) {}
    
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
    ) {}
    
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
    ) {}
    
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
    ) {}
    
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
    ) {}
    
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
    ) {}
    
    @Override
    public final void log(final String msg, final char p1) {}
    
    @Override
    public final void log(final String msg, final byte p1) {}
    
    @Override
    public final void log(final String msg, final short p1) {}
    
    @Override
    public final void log(final String msg, final int p1) {}
    
    @Override
    public final void log(final String msg, final long p1) {}
    
    @Override
    public final void log(final String msg, final Object p1, final boolean p2) {}
    
    @Override
    public final void log(final String msg, final Object p1, final char p2) {}
    
    @Override
    public final void log(final String msg, final Object p1, final byte p2) {}
    
    @Override
    public final void log(final String msg, final Object p1, final short p2) {}
    
    @Override
    public final void log(final String msg, final Object p1, final int p2) {}
    
    @Override
    public final void log(final String msg, final Object p1, final long p2) {}
    
    @Override
    public final void log(final String msg, final Object p1, final float p2) {}
    
    @Override
    public final void log(final String msg, final Object p1, final double p2) {}
    
    @Override
    public final void log(final String msg, final boolean p1, final Object p2) {}
    
    @Override
    public final void log(final String msg, final char p1, final Object p2) {}
    
    @Override
    public final void log(final String msg, final byte p1, final Object p2) {}
    
    @Override
    public final void log(final String msg, final short p1, final Object p2) {}
    
    @Override
    public final void log(final String msg, final int p1, final Object p2) {}
    
    @Override
    public final void log(final String msg, final long p1, final Object p2) {}
    
    @Override
    public final void log(final String msg, final float p1, final Object p2) {}
    
    @Override
    public final void log(final String msg, final double p1, final Object p2) {}
    
    @Override
    public final void log(final String msg, final boolean p1, final boolean p2) {}
    
    @Override
    public final void log(final String msg, final char p1, final boolean p2) {}
    
    @Override
    public final void log(final String msg, final byte p1, final boolean p2) {}
    
    @Override
    public final void log(final String msg, final short p1, final boolean p2) {}
    
    @Override
    public final void log(final String msg, final int p1, final boolean p2) {}
    
    @Override
    public final void log(final String msg, final long p1, final boolean p2) {}
    
    @Override
    public final void log(final String msg, final float p1, final boolean p2) {}
    
    @Override
    public final void log(final String msg, final double p1, final boolean p2) {}
    
    @Override
    public final void log(final String msg, final boolean p1, final char p2) {}
    
    @Override
    public final void log(final String msg, final char p1, final char p2) {}
    
    @Override
    public final void log(final String msg, final byte p1, final char p2) {}
    
    @Override
    public final void log(final String msg, final short p1, final char p2) {}
    
    @Override
    public final void log(final String msg, final int p1, final char p2) {}
    
    @Override
    public final void log(final String msg, final long p1, final char p2) {}
    
    @Override
    public final void log(final String msg, final float p1, final char p2) {}
    
    @Override
    public final void log(final String msg, final double p1, final char p2) {}
    
    @Override
    public final void log(final String msg, final boolean p1, final byte p2) {}
    
    @Override
    public final void log(final String msg, final char p1, final byte p2) {}
    
    @Override
    public final void log(final String msg, final byte p1, final byte p2) {}
    
    @Override
    public final void log(final String msg, final short p1, final byte p2) {}
    
    @Override
    public final void log(final String msg, final int p1, final byte p2) {}
    
    @Override
    public final void log(final String msg, final long p1, final byte p2) {}
    
    @Override
    public final void log(final String msg, final float p1, final byte p2) {}
    
    @Override
    public final void log(final String msg, final double p1, final byte p2) {}
    
    @Override
    public final void log(final String msg, final boolean p1, final short p2) {}
    
    @Override
    public final void log(final String msg, final char p1, final short p2) {}
    
    @Override
    public final void log(final String msg, final byte p1, final short p2) {}
    
    @Override
    public final void log(final String msg, final short p1, final short p2) {}
    
    @Override
    public final void log(final String msg, final int p1, final short p2) {}
    
    @Override
    public final void log(final String msg, final long p1, final short p2) {}
    
    @Override
    public final void log(final String msg, final float p1, final short p2) {}
    
    @Override
    public final void log(final String msg, final double p1, final short p2) {}
    
    @Override
    public final void log(final String msg, final boolean p1, final int p2) {}
    
    @Override
    public final void log(final String msg, final char p1, final int p2) {}
    
    @Override
    public final void log(final String msg, final byte p1, final int p2) {}
    
    @Override
    public final void log(final String msg, final short p1, final int p2) {}
    
    @Override
    public final void log(final String msg, final int p1, final int p2) {}
    
    @Override
    public final void log(final String msg, final long p1, final int p2) {}
    
    @Override
    public final void log(final String msg, final float p1, final int p2) {}
    
    @Override
    public final void log(final String msg, final double p1, final int p2) {}
    
    @Override
    public final void log(final String msg, final boolean p1, final long p2) {}
    
    @Override
    public final void log(final String msg, final char p1, final long p2) {}
    
    @Override
    public final void log(final String msg, final byte p1, final long p2) {}
    
    @Override
    public final void log(final String msg, final short p1, final long p2) {}
    
    @Override
    public final void log(final String msg, final int p1, final long p2) {}
    
    @Override
    public final void log(final String msg, final long p1, final long p2) {}
    
    @Override
    public final void log(final String msg, final float p1, final long p2) {}
    
    @Override
    public final void log(final String msg, final double p1, final long p2) {}
    
    @Override
    public final void log(final String msg, final boolean p1, final float p2) {}
    
    @Override
    public final void log(final String msg, final char p1, final float p2) {}
    
    @Override
    public final void log(final String msg, final byte p1, final float p2) {}
    
    @Override
    public final void log(final String msg, final short p1, final float p2) {}
    
    @Override
    public final void log(final String msg, final int p1, final float p2) {}
    
    @Override
    public final void log(final String msg, final long p1, final float p2) {}
    
    @Override
    public final void log(final String msg, final float p1, final float p2) {}
    
    @Override
    public final void log(final String msg, final double p1, final float p2) {}
    
    @Override
    public final void log(final String msg, final boolean p1, final double p2) {}
    
    @Override
    public final void log(final String msg, final char p1, final double p2) {}
    
    @Override
    public final void log(final String msg, final byte p1, final double p2) {}
    
    @Override
    public final void log(final String msg, final short p1, final double p2) {}
    
    @Override
    public final void log(final String msg, final int p1, final double p2) {}
    
    @Override
    public final void log(final String msg, final long p1, final double p2) {}
    
    @Override
    public final void log(final String msg, final float p1, final double p2) {}
    
    @Override
    public final void log(final String msg, final double p1, final double p2) {}
    
}
