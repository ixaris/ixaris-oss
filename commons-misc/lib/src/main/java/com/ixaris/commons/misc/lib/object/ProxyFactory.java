package com.ixaris.commons.misc.lib.object;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * When using Proxy.newInstance(), Java will do 2 things; create a proxy class and instantiate it. The most expensive
 * part of this process is the dynamic creation of the proxy class. This class can be used to create the proxy class
 * once and instantiate it multiple times, avoiding the cost of creating the dynamic class for every instantiation.
 *
 * @author brian.vella
 */
public class ProxyFactory {
    
    public static ProxyFactory getOrCreateProxyFactory(final Class<?> iface) {
        return getOrCreateProxyFactory(iface.getClassLoader(), iface);
    }
    
    public static ProxyFactory getOrCreateProxyFactory(final ClassLoader cl, final Class<?>... ifaces) {
        
        final Set<Class<?>> interfacesSet = new HashSet<>(Arrays.asList(ifaces));
        ProxyFactory pf;
        PROXY_FACTORIES_LOCK.readLock().lock();
        try {
            pf = PROXY_FACTORIES.get(interfacesSet);
        } finally {
            PROXY_FACTORIES_LOCK.readLock().unlock();
        }
        if (pf == null) {
            PROXY_FACTORIES_LOCK.writeLock().lock();
            try {
                pf = PROXY_FACTORIES.get(interfacesSet);
                if (pf == null) {
                    pf = new ProxyFactory(cl, ifaces);
                    PROXY_FACTORIES.put(interfacesSet, pf);
                }
            } finally {
                PROXY_FACTORIES_LOCK.writeLock().unlock();
            }
        }
        return pf;
    }
    
    private static Map<Set<Class<?>>, ProxyFactory> PROXY_FACTORIES = new HashMap<>();
    private static ReentrantReadWriteLock PROXY_FACTORIES_LOCK = new ReentrantReadWriteLock();
    
    private final Constructor<?> ctorRef;
    
    /**
     * Creates a factory for proxy objects that implement the specified interface.
     */
    public ProxyFactory(final ClassLoader loader, final Class<?>... interfaces) {
        
        try {
            ctorRef = Proxy.getProxyClass(loader, interfaces).getConstructor(InvocationHandler.class);
        } catch (NoSuchMethodException e) {
            throw new InternalError(e.toString(), e);
        }
    }
    
    /**
     * Returns an instance of a proxy class for this factory's interfaces that dispatches method invocations to the
     * specified invocation handler. <tt>ProxyFactory.newInstance</tt> throws <tt>IllegalArgumentException</tt> for the
     * same reasons that <tt>Proxy.getProxyClass</tt> does.
     *
     * @param handler the invocation handler to dispatch method invocations to
     * @return a proxy instance with the specified invocation handler of a proxy class that implements this factory's
     *     specified interfaces
     * @throws IllegalArgumentException if any of the restrictions on the parameters that may be passed to
     *     <tt>getProxyClass</tt> are violated
     * @throws NullPointerException if the invocation handler is null
     */
    public Object newInstance(final InvocationHandler handler) {
        try {
            return ctorRef.newInstance(handler);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new InternalError(e.toString(), e);
        }
    }
    
}
