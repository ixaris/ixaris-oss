package com.ixaris.commons.misc.lib.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionUtil {
    
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> RuntimeException sneakyThrow(final Throwable t) throws T {
        throw (T) t;
    }

    /**
     * Extracted from commons-lang
     *
     * @param t
     * @return
     */
    public static String getStackTrace(final Throwable t) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
    
    private ExceptionUtil() {}
    
}
