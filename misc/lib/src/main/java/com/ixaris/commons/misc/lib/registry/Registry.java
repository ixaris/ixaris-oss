package com.ixaris.commons.misc.lib.registry;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.ixaris.commons.misc.lib.object.GenericsUtil;

/**
 * Base registry class. Registers registerable values and provides reverse resolution from a string to the registered value of type V for the
 * given key
 *
 * <p>Items should be removed from the registry with care. Keep in mind what would happen if an item that is no longer registered is needed.
 * Consider either migrating the data, or providing some alternative, possibly a legacy implementation of the item.
 *
 * <p>Support for this is provided via the {@link Archived} annotation, which marks an item as 'hidden' from view via {@link
 * Registry#getRegisteredKeys()} or {@link Registry#getRegisteredValues()} but still resolvable via {@link Registry#resolve(Class, String)}.
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public abstract class Registry<V extends Registerable> {
    
    private static final Map<Class<? extends Registerable>, Registry<? extends Registerable>> REGISTRY_MAP = new HashMap<>();
    private static final Map<Class<? extends Registerable>, Set<? extends Registerable>> PENDING_VALUES = new HashMap<>();
    
    public static void registerInApplicableRegistries(final Registerable value) {
        if (value.getClass().isAnnotationPresent(DoNotRegister.class)) {
            return;
        }
        for (Class<? extends Registerable> registerableInterface : getRegisterableInterfaces(value.getClass())) {
            register(registerableInterface, value);
        }
    }
    
    public static void registerInApplicableRegistries(final RegisterableEnum regEnum) {
        if (regEnum.getClass().isAnnotationPresent(DoNotRegister.class)) {
            return;
        }
        for (final Registerable value : regEnum.getEnumValues()) {
            for (Class<? extends Registerable> registerableInterface : getRegisterableInterfaces(value.getClass())) {
                register(registerableInterface, value);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends Registerable> void register(final Class<T> type, final Registerable value) {
        final T v = (T) value;
        final Registry<T> registry;
        synchronized (REGISTRY_MAP) {
            registry = (Registry<T>) REGISTRY_MAP.get(type);
            
            if (registry != null) {
                // registry exists, so register immediately
                registry.register(v);
            } else {
                // keep values pending until the registry is added
                Set<T> pendingValuesForThisType = (Set<T>) PENDING_VALUES.get(type);
                
                if (pendingValuesForThisType == null) {
                    pendingValuesForThisType = new HashSet<>();
                    PENDING_VALUES.put(type, pendingValuesForThisType);
                }
                
                if (pendingValuesForThisType.contains(v)) {
                    throw new IllegalStateException("Trying to register key ["
                        + v.getKey()
                        + "] which is already registered for type ["
                        + type
                        + "]");
                }
                pendingValuesForThisType.add(v);
            }
        }
    }
    
    public static void unregisterFromApplicableRegistries(final Registerable value) {
        if (value.getClass().isAnnotationPresent(DoNotRegister.class)) {
            return;
        }
        for (Class<? extends Registerable> registerableInterface : getRegisterableInterfaces(value.getClass())) {
            unregister(registerableInterface, value);
        }
    }
    
    public static void unregisterInApplicableRegistries(final RegisterableEnum regEnum) {
        if (regEnum.getClass().isAnnotationPresent(DoNotRegister.class)) {
            return;
        }
        for (final Registerable value : regEnum.getEnumValues()) {
            for (Class<? extends Registerable> registerableInterface : getRegisterableInterfaces(value.getClass())) {
                unregister(registerableInterface, value);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends Registerable> void unregister(final Class<T> type, final Registerable value) {
        final T v = (T) value;
        final Registry<T> registry;
        synchronized (REGISTRY_MAP) {
            registry = (Registry<T>) REGISTRY_MAP.get(type);
            
            if (registry != null) {
                // registry exists, so unregister immediately
                registry.unregister(v);
            } else {
                // remove from pending values
                final Set<T> pendingValuesForThisType = (Set<T>) PENDING_VALUES.get(type);
                
                if (pendingValuesForThisType != null) {
                    pendingValuesForThisType.remove(v);
                    if (pendingValuesForThisType.isEmpty()) {
                        PENDING_VALUES.remove(type);
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static Set<Class<Registerable>> getRegisterableInterfaces(final Class<?> clazz) {
        final Set<Class<Registerable>> result = new HashSet<>();
        // we iterate from this class to the superclass, stopping if we get to AbstractRegisterable or Object
        // which would mean there are no more meaningful interfaces to find#
        Class<?> c = clazz;
        while ((c != Object.class) && (c != AbstractRegisterable.class)) {
            result.addAll(getRegisterableInterfacesForType(c));
            c = c.getSuperclass();
        }
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private static Set<Class<Registerable>> getRegisterableInterfacesForType(final Class<?> clazz) {
        final Set<Class<Registerable>> result = new HashSet<>();
        
        // we iterate through the implemented / extended interfaces
        for (Class<?> implInterface : clazz.getInterfaces()) {
            // since an interface may be implemented / extended at various levels in the hierarchy,
            // we stop if this interface has already been added to the set
            // if the interface is indeed registerable, we add it and any superinterface that is itself registerable
            if (!result.contains(implInterface) && (implInterface != Registerable.class) && Registerable.class.isAssignableFrom(implInterface)) {
                if (!implInterface.isAnnotationPresent(NoRegistry.class)) {
                    result.add((Class<Registerable>) implInterface);
                }
                result.addAll(getRegisterableInterfacesForType(implInterface));
            }
        }
        
        return result;
    }
    
    /**
     * Reverse resolves a registered value from a key by matching the registry to the type
     *
     * @param clazz the registerable type
     * @param key the key, should not be null
     * @return the resolved value, never null
     */
    @SuppressWarnings("unchecked")
    public static <T extends Registerable> T resolve(final Class<T> clazz, final String key) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz is null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        
        synchronized (REGISTRY_MAP) {
            final Registry<T> registry = (Registry<T>) REGISTRY_MAP.get(clazz);
            if (registry != null) {
                return registry.resolve(key);
            } else {
                throw new IllegalStateException("No registry found for type [" + clazz.getSimpleName() + "]");
            }
        }
    }
    
    private static final int DEFAULT_MIN_KEY_LENGTH = 1;
    private static final int DEFAULT_MAX_KEY_LENGTH = 255;
    
    private final Class<V> valueType;
    private final int minKeyLength;
    private final int maxKeyLength;
    
    private final Map<String, V> registeredValues = new HashMap<>();
    private final Map<String, V> archivedValues = new HashMap<>();
    
    public Registry() {
        this(DEFAULT_MIN_KEY_LENGTH, DEFAULT_MAX_KEY_LENGTH);
    }
    
    @SuppressWarnings("unchecked")
    public Registry(final int minKeyLength, final int maxKeyLength) {
        if (minKeyLength <= 0) {
            throw new IllegalArgumentException("minKeyLength should be > 0. Given " + minKeyLength);
        }
        if (maxKeyLength < minKeyLength) {
            throw new IllegalArgumentException("maxKeyLength should be >= minKeyLength. Given min is "
                + maxKeyLength
                + ", given max is "
                + maxKeyLength);
        }
        
        final Type type = GenericsUtil.getGenericTypeArguments(getClass(), Registry.class).get("V");
        if (type instanceof ParameterizedType) {
            this.valueType = (Class<V>) ((ParameterizedType) type).getRawType();
        } else {
            this.valueType = (Class<V>) type;
        }
        
        this.minKeyLength = minKeyLength;
        this.maxKeyLength = maxKeyLength;
    }
    
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void postConstruct() {
        final Set<V> pendingSet;
        synchronized (REGISTRY_MAP) {
            REGISTRY_MAP.put(valueType, this);
            
            if (PENDING_VALUES.containsKey(valueType)) {
                pendingSet = (Set<V>) PENDING_VALUES.remove(valueType);
            } else {
                pendingSet = null;
            }
        }
        
        if (pendingSet != null) {
            for (V value : pendingSet) {
                register(value);
            }
        }
    }
    
    @PreDestroy
    public void preDestroy() { // NOSONAR called by spring
        synchronized (REGISTRY_MAP) {
            REGISTRY_MAP.remove(valueType);
        }
        
        registeredValues.clear();
        archivedValues.clear();
    }
    
    /**
     * Reverse resolves a registered value from a key
     *
     * @param key the key, should not be null
     * @return the resolved value, never null
     */
    public V resolve(final String key) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        
        V resolved = registeredValues.get(key);
        
        if (resolved != null) {
            return resolved;
        }
        
        // if we are resolving from a key, we need to check if there are any archived values which match this key
        // - likely scenario is that persisted data is being retrieved. Even if the functionality has been archived, it
        // still needs to be viewable!
        resolved = archivedValues.get(key);
        if (resolved != null) {
            return resolved;
        }
        
        throw new IllegalStateException("Key [" + key + "] is not registered");
    }
    
    /**
     * Register a value
     *
     * @param value the value to register
     * @throws IllegalArgumentException when registering a value that violates key constraints
     * @throws IllegalStateException when registering a value that was previously registered
     */
    private synchronized void register(final V value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (value.getKey() == null) {
            throw new IllegalArgumentException("key should not be null");
        }
        if (value.getKey().length() < minKeyLength) {
            throw new IllegalArgumentException("Key [" + value.getKey() + "] too short. Min length allowed is " + minKeyLength);
        }
        if (value.getKey().length() > maxKeyLength) {
            throw new IllegalArgumentException("Key [" + value.getKey() + "] too long. Max length allowed is " + maxKeyLength);
        }
        if (registeredValues.containsKey(value.getKey()) || archivedValues.containsKey(value.getKey())) {
            throw new IllegalStateException("Trying to register key ["
                + value.getKey()
                + "] which is already registered for type ["
                + valueType
                + "]");
        }
        
        if (value.getClass().isAnnotationPresent(Archived.class)) {
            archivedValues.put(value.getKey(), value);
        } else {
            registeredValues.put(value.getKey(), value);
        }
        
        onRegister(value);
    }
    
    /**
     * On register callback
     *
     * @param value
     */
    protected void onRegister(final V value) {}
    
    /**
     * Unregister a value
     *
     * @param value the value to unregister
     * @throws IllegalStateException when unregistering a value that was not previously registered
     */
    private synchronized void unregister(final V value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (!registeredValues.containsKey(value.getKey()) && !archivedValues.containsKey(value.getKey())) {
            throw new IllegalStateException("Trying to unregister key [" + value.getKey() + "] which is not registered");
        }
        
        if (value.getClass().isAnnotationPresent(Archived.class)) {
            archivedValues.remove(value.getKey());
        } else {
            registeredValues.remove(value.getKey());
        }
        
        onUnregister(value);
    }
    
    /**
     * On unregister callback
     *
     * @param value
     */
    protected void onUnregister(final V value) {}
    
    /**
     * @return the collection of registered values. Note that archived values are not retrieved, and in general should not be retrieved.
     */
    public Collection<V> getRegisteredValues() {
        return Collections.unmodifiableCollection(registeredValues.values());
    }
    
    /**
     * @return the set of registered keys. As in the case for values, archived keys are not retrieved, and in general should not be retrieved.
     */
    public Set<String> getRegisteredKeys() {
        return Collections.unmodifiableSet(registeredValues.keySet());
    }
    
    /**
     * @return the set of archived keys. Useful for areas which require historical data.
     */
    public Set<String> getArchivedKeys() {
        return Collections.unmodifiableSet(archivedValues.keySet());
    }
    
}
