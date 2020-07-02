package com.ixaris.commons.jooq.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jooq.DSLContext;

import com.ixaris.commons.misc.lib.object.ProxyFactory;
import com.ixaris.commons.misc.lib.object.ReflectionUtils;
import com.ixaris.commons.persistence.lib.Transaction;
import com.ixaris.commons.persistence.lib.TransactionalPersistenceContext;

/**
 * Creates instances that implement {@link TransactionalDSLContext}, which in reality are proxies to an instance implementing {@link DSLContext}
 * and an instance of {@link Transaction}
 */
public class TransactionalDSLContextFactory {
    
    private static final ProxyFactory PROXY_FACTORY = new ProxyFactory(TransactionalDSLContextFactory.class
        .getClassLoader(), // NOSONAR this is the correct classloader
        DSLContext.class,
        TransactionalDSLContext.class);
    
    private static final class TransactionalDSLContextInvocationHandler implements InvocationHandler {
        
        private final Transaction transaction;
        private final DSLContext dslContext;
        
        private TransactionalDSLContextInvocationHandler(final Transaction transaction, final DSLContext dslContext) {
            this.transaction = transaction;
            this.dslContext = dslContext;
        }
        
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable { // NOSONAR implementing interface
            if (ReflectionUtils.isEqualsMethod(method)) {
                // Only consider equal when proxies are identical.
                return proxy == args[0]; // NOSONAR explicitly checking reference
                
            } else if (method.getDeclaringClass().equals(TransactionalPersistenceContext.class)) {
                try {
                    return method.invoke(transaction, args);
                } catch (final InvocationTargetException e) {
                    throw e.getCause();
                }
                
            } else {
                try {
                    return method.invoke(dslContext, args);
                } catch (final InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }
        
    }
    
    public static TransactionalDSLContext create(final Transaction tx, final DSLContext ctx) {
        return (TransactionalDSLContext) PROXY_FACTORY.newInstance(new TransactionalDSLContextInvocationHandler(tx, ctx));
    }
    
    private TransactionalDSLContextFactory() {}
}
