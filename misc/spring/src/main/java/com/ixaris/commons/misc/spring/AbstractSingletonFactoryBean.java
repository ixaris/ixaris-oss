package com.ixaris.commons.misc.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.util.ReflectionUtils;

import com.ixaris.commons.misc.lib.object.GenericsUtil;

/**
 * This factory bean allows us to give a pre created singleton instance to the spring context. These are then treated as normal beans by spring
 * (e.g. can be autowired etc.)
 *
 * <p>Use as follows:
 *
 * <pre>
 * public class SomeSingleton  {
 *
 *     private static final SomeSingleton INSTANCE = new SomeSingleton();
 *
 *     public static SomeSingleton getInstance() {
 *         return INSTANCE;
 *     }
 *
 *     private SomeSingleton() { }
 *
 *     ...
 *
 *     &#064;Component
 *     public static final class SomeSingletonFactoryBean extends AbstractSingletonFactoryBean<SomeSingleton> { }
 *
 * }
 * </pre>
 *
 * @author brian.vella
 */
public abstract class AbstractSingletonFactoryBean<T> implements SmartFactoryBean<T>, InitializingBean, DisposableBean {
    
    private static final Log LOG = LogFactory.getLog(AbstractSingletonFactoryBean.class);
    
    private final Class<T> type;
    
    private final T instance;
    
    private LifecycleMetadata metadata;
    
    @SuppressWarnings("unchecked")
    protected AbstractSingletonFactoryBean() {
        type = (Class<T>) GenericsUtil.getGenericTypeArguments(getClass(), AbstractSingletonFactoryBean.class).get("T");
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalStateException("Type [" + type + "] is an interface or abstract class");
        }
        instance = getInstance();
    }
    
    @SuppressWarnings("unchecked")
    public T getInstance() {
        try {
            return (T) type.getMethod("getInstance").invoke(null);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Error while calling getInstance() on " + type.getSimpleName(), e);
        }
    }
    
    /**
     * Used in case the instance used is an InitializingBean
     *
     * @see InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (instance instanceof InitializingBean) {
            ((InitializingBean) instance).afterPropertiesSet();
        }
    }
    
    /**
     * Used in case the instance used is a DisposableBean
     *
     * @see DisposableBean#destroy()
     */
    @Override
    public void destroy() throws Exception {
        if (instance instanceof DisposableBean) {
            ((DisposableBean) instance).destroy();
        }
    }
    
    @PostConstruct
    public void postConstruct() {
        // call all Post Construct methods in the class in question and it's parents
        final LifecycleMetadata lifeMetadata = buildLifecycleMetadata(type);
        try {
            lifeMetadata.invokeInitMethods(instance, instance.getClass().getName());
        } catch (final InvocationTargetException e) {
            LOG.warn("Invocation of init method failed on bean with name " + instance.getClass().getName(), e.getTargetException());
        } catch (final IllegalAccessException | RuntimeException e) {
            LOG.error("Couldn't invoke init method on bean with name " + instance.getClass().getName(), e);
        }
    }
    
    @PreDestroy
    public void preDestroy() {
        // call all Pre Destroy methods in the class in question and it's parents
        final LifecycleMetadata lifeMetadata = buildLifecycleMetadata(type);
        try {
            lifeMetadata.invokeDestroyMethods(instance, instance.getClass().getName());
        } catch (final InvocationTargetException e) {
            LOG.warn("Invocation of destroy method failed on bean with name " + instance.getClass().getName(), e.getTargetException());
        } catch (final IllegalAccessException | RuntimeException e) {
            LOG.error("Couldn't invoke destroy method on bean with name " + instance.getClass().getName(), e);
        }
    }
    
    @Override
    public final T getObject() {
        return instance;
    }
    
    @Override
    public final Class<?> getObjectType() {
        return type;
    }
    
    @Override
    public final boolean isSingleton() {
        return true;
    }
    
    @Override
    public final boolean isPrototype() {
        return false;
    }
    
    @Override
    public final boolean isEagerInit() {
        return true;
    }
    
    /**
     * Creates a LifecycleMetadata which contains the list of {@link PostConstruct} and {@link PreDestroy} methods in the class and it's parent
     *
     * @param clazz the class for which the metadata is being populated
     * @return the LifecycleMetadata for the class
     */
    private LifecycleMetadata buildLifecycleMetadata(final Class<?> clazz) {
        if (metadata != null) {
            // If metadata has already been generated, we do not regenerate it
            return metadata;
        }
        final List<LifecycleElement> initMethods = new ArrayList<>();
        final List<LifecycleElement> destroyMethods = new ArrayList<>();
        Class<?> targetClass = clazz;
        
        do {
            final List<LifecycleElement> currInitMethods = new ArrayList<>();
            final List<LifecycleElement> currDestroyMethods = new ArrayList<>();
            for (final Method method : targetClass.getDeclaredMethods()) {
                if (method.getAnnotation(PostConstruct.class) != null) {
                    final LifecycleElement element = new LifecycleElement(method);
                    currInitMethods.add(element);
                    LOG.debug("Found init method on class [" + clazz.getName() + "]: " + method);
                }
                if (method.getAnnotation(PreDestroy.class) != null) {
                    currDestroyMethods.add(new LifecycleElement(method));
                    LOG.debug("Found destroy method on class [" + clazz.getName() + "]: " + method);
                }
            }
            // add all @PostConstruct methods in order such that the parent's Post Constructs are called first
            initMethods.addAll(0, currInitMethods);
            
            // add all @PreDestroy methods such that the children's Pre Destroys are called first
            destroyMethods.addAll(currDestroyMethods);
            
            // keep iterating for parents until Object is reached
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);
        
        metadata = new LifecycleMetadata(initMethods, destroyMethods);
        return metadata;
    }
    
    /**
     * Class representing information about annotated init and destroy methods.
     */
    private static class LifecycleMetadata {
        
        private final Collection<LifecycleElement> initMethods;
        
        private final Collection<LifecycleElement> destroyMethods;
        
        public LifecycleMetadata(final Collection<LifecycleElement> initMethods, final Collection<LifecycleElement> destroyMethods) {
            
            this.initMethods = initMethods;
            this.destroyMethods = destroyMethods;
        }
        
        public void invokeInitMethods(final Object target, final String beanName) throws InvocationTargetException, IllegalAccessException {
            if (!initMethods.isEmpty()) {
                for (final LifecycleElement element : initMethods) {
                    LOG.debug("Invoking init method on bean '" + beanName + "': " + element.getMethod());
                    element.invoke(target);
                }
            }
        }
        
        public void invokeDestroyMethods(final Object target, final String beanName) throws InvocationTargetException, IllegalAccessException {
            if (!destroyMethods.isEmpty()) {
                for (final LifecycleElement element : destroyMethods) {
                    LOG.debug("Invoking destroy method on bean '" + beanName + "': " + element.getMethod());
                    element.invoke(target);
                }
            }
        }
    }
    
    /**
     * Class representing injection information about an annotated method.
     */
    private static class LifecycleElement {
        
        private final Method method;
        
        private final String identifier;
        
        public LifecycleElement(final Method method) {
            if (method.getParameterTypes().length != 0) {
                throw new IllegalStateException("Lifecycle method annotation requires a no-arg method: " + method);
            }
            this.method = method;
            identifier = Modifier.isPrivate(method.getModifiers()) ? method.getDeclaringClass() + "." + method.getName() : method.getName();
        }
        
        public Method getMethod() {
            return method;
        }
        
        public void invoke(final Object target) throws InvocationTargetException, IllegalAccessException {
            ReflectionUtils.makeAccessible(method);
            method.invoke(target, (Object[]) null);
        }
        
        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof LifecycleElement)) {
                return false;
            }
            final LifecycleElement otherElement = (LifecycleElement) other;
            return identifier.equals(otherElement.identifier);
        }
        
        @Override
        public int hashCode() {
            return identifier.hashCode();
        }
    }
}
