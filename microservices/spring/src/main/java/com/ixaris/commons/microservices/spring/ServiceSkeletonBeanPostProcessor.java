package com.ixaris.commons.microservices.spring;

import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.core.Ordered;

import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;

/**
 * Initialises and destroys the service skeleton beans using {@link ServiceSupport} using Spring lifecycles
 */
public final class ServiceSkeletonBeanPostProcessor implements DestructionAwareBeanPostProcessor, Ordered {
    
    private final ServiceSupport serviceSupport;
    
    public ServiceSkeletonBeanPostProcessor(final ServiceSupport serviceSupport) {
        
        if (serviceSupport == null) {
            throw new IllegalArgumentException("serviceSupport is null");
        }
        
        this.serviceSupport = serviceSupport;
    }
    
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
    
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        if (bean instanceof ServiceSkeleton) {
            serviceSupport.init((ServiceSkeleton) bean);
        }
        
        return bean;
    }
    
    @Override
    public boolean requiresDestruction(final Object bean) {
        return bean instanceof ServiceSkeleton;
    }
    
    @Override
    public void postProcessBeforeDestruction(final Object bean, final String beanName) {
        if (bean instanceof ServiceSkeleton) {
            serviceSupport.destroy((ServiceSkeleton) bean);
        }
    }
    
}
