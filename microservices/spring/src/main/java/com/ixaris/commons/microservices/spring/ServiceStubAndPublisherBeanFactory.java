package com.ixaris.commons.microservices.spring;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.service.ServiceEventPublisher;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;
import com.ixaris.commons.misc.lib.object.ClassUtil;
import com.ixaris.commons.misc.lib.object.Tuple2;

final class ServiceStubAndPublisherBeanFactory implements ListableBeanFactory {
    
    private static final String[] EMPTY_STRING_ARR = new String[0];
    
    private final ServiceSupport serviceSupport;
    private final ServiceClientSupport serviceClientSupport;
    private final Map<Class<?>, Object> proxiesByType = new HashMap<>();
    private final Map<String, Object> proxiesByName = new HashMap<>();
    
    ServiceStubAndPublisherBeanFactory(final ServiceSupport serviceSupport, final ServiceClientSupport serviceClientSupport) {
        this.serviceSupport = serviceSupport;
        this.serviceClientSupport = serviceClientSupport;
    }
    
    @Override
    public boolean containsBeanDefinition(final String beanName) {
        return proxiesByName.containsKey(beanName);
    }
    
    @Override
    public int getBeanDefinitionCount() {
        return proxiesByName.size();
    }
    
    @Override
    public String[] getBeanDefinitionNames() {
        return proxiesByName.keySet().toArray(new String[0]);
    }
    
    @Override
    public String[] getBeanNamesForType(final ResolvableType type) {
        return getBeanNamesForType(type.getRawClass());
    }
    
    @Override
    public String[] getBeanNamesForType(final ResolvableType type, final boolean includeNonSingletons, final boolean allowEagerInit) {
        return getBeanNamesForType(type.getRawClass());
    }
    
    @Override
    public String[] getBeanNamesForType(final Class<?> type) {
        final Tuple2<String, Object> proxy = findProxy(type);
        if (proxy != null) {
            return new String[] { proxy.get1() };
        } else {
            return EMPTY_STRING_ARR;
        }
    }
    
    @Override
    public String[] getBeanNamesForType(final Class<?> type, final boolean includeNonSingletons, final boolean allowEagerInit) {
        return getBeanNamesForType(type);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> Map<String, T> getBeansOfType(final Class<T> type) throws BeansException {
        final Tuple2<String, Object> proxy = findProxy(type);
        if (proxy != null) {
            return Collections.singletonMap(proxy.get1(), (T) proxy.get2());
        } else {
            return Collections.emptyMap();
        }
    }
    
    @Override
    public <T> Map<String, T> getBeansOfType(final Class<T> type, final boolean includeNonSingletons, final boolean allowEagerInit) throws BeansException {
        return getBeansOfType(type);
    }
    
    @Override
    public String[] getBeanNamesForAnnotation(final Class<? extends Annotation> annotationType) {
        return new String[0];
    }
    
    @Override
    public Map<String, Object> getBeansWithAnnotation(final Class<? extends Annotation> annotationType) {
        return Collections.emptyMap();
    }
    
    @Override
    public <A extends Annotation> A findAnnotationOnBean(final String beanName, final Class<A> annotationType) {
        return null;
    }
    
    @Override
    public Object getBean(final String name) throws BeansException {
        final Object proxy = proxiesByName.get(name);
        if (proxy != null) {
            return proxy;
        } else {
            throw new NoSuchBeanDefinitionException(name);
        }
    }
    
    @Override
    public <T> T getBean(final String name, final Class<T> requiredType) throws BeansException {
        if (requiredType != null) {
            return getBean(requiredType);
        } else {
            throw new NoSuchBeanDefinitionException(name);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBean(final Class<T> requiredType) throws BeansException {
        final Tuple2<String, Object> proxy = findProxy(requiredType);
        if (proxy != null) {
            return (T) proxy.get2();
        } else {
            throw new NoSuchBeanDefinitionException(requiredType);
        }
    }
    
    @Override
    public Object getBean(final String name, final Object... args) throws BeansException {
        return getBean(name);
    }
    
    @Override
    public <T> T getBean(final Class<T> requiredType, final Object... args) throws BeansException {
        return getBean(requiredType);
    }
    
    @Override
    public <T> ObjectProvider<T> getBeanProvider(final Class<T> requiredType) {
        return new ObjectProvider<T>() {
            @Override
            public T getObject() throws BeansException {
                return getBean(requiredType);
            }
            
            @Override
            public T getObject(final Object... args) throws BeansException {
                return getBean(requiredType, args);
            }
            
            @Override
            public T getIfAvailable() throws BeansException {
                try {
                    return getBean(requiredType);
                } catch (@SuppressWarnings("squid:S1166") final NoSuchBeanDefinitionException e) {
                    return null;
                }
            }
            
            @Override
            public T getIfUnique() throws BeansException {
                try {
                    return getBean(requiredType);
                } catch (@SuppressWarnings("squid:S1166") final NoSuchBeanDefinitionException e) {
                    return null;
                }
            }
        };
        
    }
    
    @Override
    public <T> ObjectProvider<T> getBeanProvider(final ResolvableType requiredType) {
        return new ObjectProvider<T>() {
            @Override
            public T getObject() throws BeansException {
                final String[] beanNames = getBeanNamesForType(requiredType);
                if (beanNames.length == 1) {
                    return (T) getBean(beanNames[0], requiredType);
                }
                if (beanNames.length > 1) {
                    throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
                }
                throw new NoSuchBeanDefinitionException(requiredType);
            }
            
            @Override
            public T getObject(final Object... args) throws BeansException {
                final String[] beanNames = getBeanNamesForType(requiredType);
                if (beanNames.length == 1) {
                    return (T) getBean(beanNames[0], args);
                }
                if (beanNames.length > 1) {
                    throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
                }
                throw new NoSuchBeanDefinitionException(requiredType);
            }
            
            @Override
            public T getIfAvailable() throws BeansException {
                final String[] beanNames = getBeanNamesForType(requiredType);
                if (beanNames.length == 1) {
                    return (T) getBean(beanNames[0]);
                }
                if (beanNames.length > 1) {
                    throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
                }
                return null;
            }
            
            @Override
            public T getIfUnique() throws BeansException {
                final String[] beanNames = getBeanNamesForType(requiredType);
                if (beanNames.length == 1) {
                    return (T) getBean(beanNames[0]);
                }
                return null;
            }
        };
    }
    
    @Override
    public boolean containsBean(final String name) {
        return proxiesByName.containsKey(name);
    }
    
    @Override
    public boolean isSingleton(final String name) throws NoSuchBeanDefinitionException {
        final Object proxy = proxiesByName.get(name);
        if (proxy != null) {
            return true;
        } else {
            throw new NoSuchBeanDefinitionException(name);
        }
    }
    
    @Override
    public boolean isPrototype(final String name) throws NoSuchBeanDefinitionException {
        return !isSingleton(name);
    }
    
    @Override
    public boolean isTypeMatch(final String name, final ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return isTypeMatch(name, typeToMatch.getRawClass());
    }
    
    @Override
    public boolean isTypeMatch(final String name, final Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return ClassUtil.isSameOrSubtypeOf(getType(name), typeToMatch);
    }
    
    @Override
    public Class<?> getType(final String name) throws NoSuchBeanDefinitionException {
        final Object proxy = proxiesByName.get(name);
        if (proxy != null) {
            return proxy.getClass();
        } else {
            throw new NoSuchBeanDefinitionException(name);
        }
    }
    
    @Override
    public Class<?> getType(final String name, final boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
        return getType(name);
    }
    
    @Override
    public String[] getAliases(final String name) {
        return new String[0];
    }
    
    @SuppressWarnings("unchecked")
    private <T> Tuple2<String, T> findProxy(final Class<?> requiredType) {
        if (requiredType.isInterface()) {
            if (ClassUtil.isSameOrSubtypeOf(requiredType, ServiceStub.class) && !requiredType.equals(ServiceStub.class)) {
                final String name = generateBeanName(requiredType);
                T proxy = (T) proxiesByType.get(requiredType);
                if (proxy != null) {
                    return tuple(name, proxy);
                } else {
                    proxy = createStubProxy(requiredType);
                    proxiesByType.put(requiredType, proxy);
                    proxiesByName.put(name, proxy);
                    return tuple(name, proxy);
                }
            } else if (ClassUtil.isSameOrSubtypeOf(requiredType, ServiceEventPublisher.class) && !requiredType.equals(ServiceEventPublisher.class)) {
                final String name = generateBeanName(requiredType);
                T proxy = (T) proxiesByType.get(requiredType);
                if (proxy != null) {
                    return tuple(name, proxy);
                } else {
                    proxy = createPublisherProxy(requiredType);
                    proxiesByType.put(requiredType, proxy);
                    proxiesByName.put(name, proxy);
                    return tuple(name, proxy);
                }
            }
        }
        return null;
    }
    
    private String generateBeanName(final Class<?> cls) {
        return Introspector.decapitalize(ClassUtils.getShortName(cls.getName()));
    }
    
    @SuppressWarnings("unchecked")
    private <T extends ServiceStub> T createStubProxy(final Class<?> requiredType) {
        final Class<T> type = (Class<T>) requiredType;
        return serviceClientSupport.getOrCreate(type).createProxy();
    }
    
    @SuppressWarnings("unchecked")
    private <T extends ServiceEventPublisher<?, ?, ?>> T createPublisherProxy(final Class<?> requiredType) {
        final Class<T> type = (Class<T>) requiredType;
        return serviceSupport.getOrCreate(determineSkeletonType(type)).createPublisherProxy(type);
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends ServiceSkeleton> determineSkeletonType(final Class<? extends ServiceEventPublisher<?, ?, ?>> type) {
        Class<?> parent = type;
        do {
            parent = parent.getDeclaringClass();
        } while ((parent != null) && !ServiceSkeleton.class.isAssignableFrom(parent));
        
        if (parent == null) {
            throw new IllegalStateException();
        }
        return (Class<? extends ServiceSkeleton>) parent;
    }
    
}
