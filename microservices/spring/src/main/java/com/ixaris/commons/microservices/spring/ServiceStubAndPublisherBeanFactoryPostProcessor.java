package com.ixaris.commons.microservices.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;

public final class ServiceStubAndPublisherBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    
    private final ServiceStubAndPublisherBeanFactory serviceStubAndPublisherBeanFactory;
    
    public ServiceStubAndPublisherBeanFactoryPostProcessor(final ServiceSupport serviceSupport, final ServiceClientSupport serviceClientSupport) {
        this.serviceStubAndPublisherBeanFactory = new ServiceStubAndPublisherBeanFactory(serviceSupport, serviceClientSupport);
    }
    
    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ConfigurableBeanFactory bf = beanFactory;
        BeanFactory parent;
        while ((parent = bf.getParentBeanFactory()) != null) {
            if (!(parent instanceof ConfigurableBeanFactory)) {
                throw new IllegalStateException("Not a ConfigurableBeanFactory: " + parent);
            }
            bf = (ConfigurableBeanFactory) parent;
        }
        
        bf.setParentBeanFactory(serviceStubAndPublisherBeanFactory);
    }
    
}
