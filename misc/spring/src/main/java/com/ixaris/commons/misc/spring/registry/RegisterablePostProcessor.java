package com.ixaris.commons.misc.spring.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.stereotype.Component;

import com.ixaris.commons.misc.lib.registry.Registerable;
import com.ixaris.commons.misc.lib.registry.RegisterableEnum;
import com.ixaris.commons.misc.lib.registry.Registry;
import com.ixaris.commons.misc.spring.AbstractSingletonFactoryBean;

/**
 * Indicates a registerable item should be registered automatically when loaded
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
@Component
public final class RegisterablePostProcessor implements DestructionAwareBeanPostProcessor {
    
    private static final Logger LOG = LoggerFactory.getLogger(RegisterablePostProcessor.class);
    
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        if (bean instanceof Registerable) {
            try {
                Registry.registerInApplicableRegistries((Registerable) bean);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Registered [" + bean + "] with name [" + beanName + "] and class [" + bean.getClass() + "]");
                }
            } catch (RuntimeException e) {
                throw new RegistryException("Could not register [" + bean + "] with name [" + beanName + "] and class [" + bean.getClass() + "]",
                    e);
            }
        } else if (bean instanceof RegisterableEnum) {
            try {
                Registry.registerInApplicableRegistries((RegisterableEnum) bean);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Registered [" + bean + "] with name [" + beanName + "] and class [" + bean.getClass() + "]");
                }
            } catch (RuntimeException e) {
                throw new RegistryException("Could not register [" + bean + "] with name [" + beanName + "] and class [" + bean.getClass() + "]",
                    e);
            }
        } else if (bean instanceof AbstractSingletonFactoryBean) {
            final AbstractSingletonFactoryBean<?> sfb = (AbstractSingletonFactoryBean<?>) bean;
            if (Registerable.class.isAssignableFrom(sfb.getObjectType())) {
                final Registerable r = (Registerable) sfb.getObject();
                try {
                    Registry.registerInApplicableRegistries(r);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Unregistered [" + r + "] with name [" + beanName + "] and class [" + r.getClass() + "]");
                    }
                } catch (RuntimeException e) {
                    throw new RegistryException(String.format("Could not register [%s] with name [%s] and class [%s]", r, beanName, bean.getClass()), e);
                }
            }
        }
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }
    
    @Override
    public boolean requiresDestruction(final Object bean) {
        return bean instanceof Registerable || bean instanceof RegisterableEnum || bean instanceof AbstractSingletonFactoryBean;
    }
    
    @Override
    public void postProcessBeforeDestruction(final Object bean, final String beanName) throws BeansException {
        if (bean instanceof Registerable) {
            try {
                Registry.unregisterFromApplicableRegistries((Registerable) bean);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unregistered [" + bean + "] with name [" + beanName + "] and class [" + bean.getClass() + "]");
                }
            } catch (RuntimeException e) {
                throw new RegistryException(String.format("Could not unregister [%s] with name [%s] and class [%s]", bean, beanName, bean.getClass()), e);
            }
        } else if (bean instanceof RegisterableEnum) {
            try {
                Registry.unregisterInApplicableRegistries((RegisterableEnum) bean);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Registered [" + bean + "] with name [" + beanName + "] and class [" + bean.getClass() + "]");
                }
            } catch (RuntimeException e) {
                throw new RegistryException("Could not register [" + bean + "] with name [" + beanName + "] and class [" + bean.getClass() + "]",
                    e);
            }
        } else if (bean instanceof AbstractSingletonFactoryBean) {
            final AbstractSingletonFactoryBean<?> sfb = (AbstractSingletonFactoryBean<?>) bean;
            if (Registerable.class.isAssignableFrom(sfb.getObjectType())) {
                final Registerable r = (Registerable) sfb.getObject();
                try {
                    Registry.unregisterFromApplicableRegistries(r);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Unregistered [" + r + "] with name [" + beanName + "] and class [" + r.getClass() + "]");
                    }
                } catch (RuntimeException e) {
                    throw new RegistryException(String.format("Could not unregister [%s] with name [%s] and class [%s]", r, beanName, bean.getClass()), e);
                }
            }
        }
    }
    
    public static final class RegistryException extends BeansException {
        
        private static final long serialVersionUID = 2133954734929912132L;
        
        protected RegistryException(final String msg, final Throwable cause) {
            super(msg, cause);
        }
        
    }
    
}
