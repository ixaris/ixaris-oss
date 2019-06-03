package com.ixaris.commons.misc.lib.logging;

import com.ixaris.commons.misc.lib.logging.spi.LoggerFactorySpi;
import com.ixaris.commons.misc.lib.logging.spi.Slf4jLoggerFactoryImpl;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.ServiceLoader;

public final class LoggerFactory {
    
    private static final LoggerFactorySpi LOGGER_FACTORY_IMPL;
    
    static {
        LoggerFactorySpi impl = null;
        for (final LoggerFactorySpi loaded : ServiceLoader.load(LoggerFactorySpi.class)) {
            if (impl == null) {
                impl = loaded;
            } else {
                // TODO warn using slf4j, we only use the first one
            }
        }
        LOGGER_FACTORY_IMPL = impl != null ? impl : Slf4jLoggerFactoryImpl.INSTANCE;
    }
    
    /**
     * Returns a new logger instance which parses log messages using printf format for the enclosing class using the
     * system default logging backend.
     */
    public static Logger forEnclosingClass() {
        // NOTE: It is _vital_ that getCallerClass() is made directly from here
        // final Class<?> caller = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        final Class<?> caller = getCallerClass(LoggerFactory.class, new Throwable());
        if (caller != null) {
            // This might contain '$' for inner/nested classes, but that's okay.
            return new Logger(LOGGER_FACTORY_IMPL.getLogger(caller));
        }
        
        throw new IllegalStateException("no caller found on the stack");
    }
    
    /**
     * Returns the stack trace element of the immediate caller of the specified class.
     *
     * @param target the target class whose callers we are looking for.
     * @param throwable a new Throwable made at a known point in the call hierarchy.
     * @return the stack trace element representing the immediate caller of the specified class, or null if no caller
     *     was found (due to incorrect target, wrong skip count or use of JNI).
     */
    @SuppressWarnings({ "squid:S2221", "squid:S1166", "squid:S2658", "squid:S2259" })
    public static Class<?> getCallerClass(final Class<?> target, final Throwable throwable) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(throwable);
        
        // Getting the full stack trace is expensive, so avoid it where possible.
        final StackTraceElement[] stack = (FastStackGetter.INSTANCE != null) ? null : throwable.getStackTrace();
        
        // Note: To avoid having to reflect the getStackTraceDepth() method as well, we assume that we
        // will find the caller on the stack and simply catch an exception if we fail (which should
        // hardly ever happen).
        boolean foundCaller = false;
        try {
            for (int index = 0; ; index++) {
                final StackTraceElement element = (FastStackGetter.INSTANCE != null)
                    ? FastStackGetter.INSTANCE.getStackTraceElement(throwable, index) : stack[index];
                if (target.getName().equals(element.getClassName())) {
                    foundCaller = true;
                } else if (foundCaller) {
                    return Class.forName(element.getClassName());
                }
            }
        } catch (final Exception e) {
            // This should only happen is the caller was not found on the stack (getting exceptions from
            // the stack trace method should never happen) and it should only be an
            // IndexOutOfBoundsException, however we don't want _anything_ to be thrown from here.
            // TODO(dbeaumont): Change to only catch IndexOutOfBoundsException and test _everything_.
            return null;
        }
    }
    
    /**
     * A helper class to abstract the complexities of dynamically invoking the {@code getStackTraceElement()} method of
     * {@code JavaLangAccess}.
     */
    private static final class FastStackGetter {
        
        private static final FastStackGetter INSTANCE = createIfSupported();
        
        /**
         * @return a new {@code FastStackGetter} if the {@code getStackTraceElement()} method of {@code JavaLangAccess}
         *     is supported on this platform, or {@code null} otherwise.
         */
        @SuppressWarnings({ "squid:S2658", "squid:S1181", "squid:S1166" })
        private static FastStackGetter createIfSupported() {
            try {
                final Object javaLangAccess = Class.forName("sun.misc.SharedSecrets")
                    .getMethod("getJavaLangAccess")
                    .invoke(null);
                // NOTE: We do not use "javaLangAccess.getClass()" here because that's the implementation,
                // not the interface and we must obtain the reflected Method from the interface directly.
                final Method getElementMethod = Class.forName("sun.misc.JavaLangAccess").getMethod(
                    "getStackTraceElement", Throwable.class, int.class
                );
                final Method getDepthMethod = Class.forName("sun.misc.JavaLangAccess").getMethod(
                    "getStackTraceDepth", Throwable.class
                );
                
                // To really be sure that we can use these later without issue, just call them now (including
                // the cast of the returned value).
                @SuppressWarnings({ "unused", "squid:S1854" })
                final StackTraceElement unusedElement = (StackTraceElement)
                    getElementMethod.invoke(javaLangAccess, new Throwable(), 0);
                @SuppressWarnings({ "unused", "squid:S1854" })
                final int unusedDepth = (int) (Integer) getDepthMethod.invoke(javaLangAccess, new Throwable());
                
                return new FastStackGetter(javaLangAccess, getElementMethod);
            } catch (final ThreadDeath t) {
                // Do not stop ThreadDeath from propagating.
                throw t;
            } catch (final Throwable t) {
                // If creation fails for any reason we return null, which results in the logger agent falling
                // back to the (much) slower getStackTrace() method.
                return null;
            }
        }
        
        /**
         * The implementation of {@code sun.misc.JavaLangAccess} for this platform (if it exists).
         */
        private final Object javaLangAccess;
        
        /**
         * The {@code getStackTraceElement(Throwable, int)} method of {@code sun.misc.JavaLangAccess}.
         */
        private final Method getElementMethod;
        
        private FastStackGetter(final Object javaLangAccess, final Method getElementMethod) {
            this.javaLangAccess = javaLangAccess;
            this.getElementMethod = getElementMethod;
        }
        
        /**
         * Mimics a direct call to {@code getStackTraceElement()} on the JavaLangAccess interface without requiring the
         * Java runtime to directly reference the method (which may not exist on some JVMs).
         */
        @SuppressWarnings("squid:S1166")
        StackTraceElement getStackTraceElement(final Throwable throwable, final int n) {
            try {
                return (StackTraceElement) getElementMethod.invoke(javaLangAccess, throwable, n);
            } catch (final InvocationTargetException e) {
                // The only case we should expect to see here normally is a wrapped IndexOutOfBoundsException.
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else if (e.getCause() instanceof Error) {
                    throw (Error) e.getCause();
                }
                // This should not be possible because the getStackTraceElement() method does not declare
                // any checked exceptions (though APIs may change over time).
                throw new IllegalStateException(e.getCause());
            } catch (final IllegalAccessException e) {
                // This should never happen because the method has been successfully invoked once already.
                throw new IllegalStateException(e);
            }
        }
        
    }
    
    private LoggerFactory() {}
    
}
