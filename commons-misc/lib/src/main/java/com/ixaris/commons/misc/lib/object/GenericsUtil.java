package com.ixaris.commons.misc.lib.object;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

/**
 * Verious generics utils, including cast helpers
 */
public final class GenericsUtil {
    
    public static Class<?> resolveGenericTypeArgument(final Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else {
            return null;
        }
    }
    
    /**
     * Resolve the generic type arguments for the targetClass as defined by thisClass. Returns a map of generic variable
     * name to type. Unresolvable variables (e.g. not explicitly defined by thisClass) are not added to the map
     *
     * @param thisClass
     * @param targetClass
     * @return
     */
    public static Map<String, Class<?>> resolveGenericTypeArguments(
        final Class<?> thisClass, final Class<?> targetClass
    ) {
        final Map<String, Type> genericTypeArguments = getGenericTypeArguments(thisClass, targetClass);
        final Map<String, Class<?>> resolvedTypeArguments = new HashMap<>(genericTypeArguments.size());
        for (final Entry<String, Type> entry : genericTypeArguments.entrySet()) {
            final Class<?> c = resolveGenericTypeArgument(entry.getValue());
            if (c != null) {
                resolvedTypeArguments.put(entry.getKey(), c);
            }
        }
        
        return resolvedTypeArguments;
    }
    
    public static Map<String, Type> getGenericTypeArguments(final Class<?> thisClass, final Class<?> targetClass) {
        return getGenericTypeArguments(thisClass, targetClass, Collections.emptyMap());
    }
    
    private static Map<String, Type> getGenericTypeArguments(
        final Class<?> thisClass, final Class<?> targetClass, final Map<String, Type> thisMap
    ) {
        final Class<?> superClass = thisClass.getSuperclass();
        final Class<?>[] thisClassInterfaces = thisClass.getInterfaces();
        
        if (superClass == null && thisClassInterfaces.length == 0) {
            return Collections.emptyMap();
        }
        
        if (superClass != null && targetClass.isAssignableFrom(superClass)) {
            final Type superType = thisClass.getGenericSuperclass();
            final Map<String, Type> superMap = processSuperGenericTypeArgument(superType, superClass, thisMap);
            
            if (targetClass.equals(superClass)) {
                return superMap;
            } else {
                return getGenericTypeArguments(superClass, targetClass, superMap);
            }
            
        } else {
            final Type[] interfaceTypes = thisClass.getGenericInterfaces();
            for (int ii = 0; ii < thisClassInterfaces.length; ii++) {
                if (targetClass.isAssignableFrom(thisClassInterfaces[ii])) {
                    final Map<String, Type> superMap = processSuperGenericTypeArgument(
                        interfaceTypes[ii], thisClassInterfaces[ii], thisMap
                    );
                    
                    if (targetClass.equals(thisClassInterfaces[ii])) {
                        return superMap;
                    } else {
                        return getGenericTypeArguments(thisClassInterfaces[ii], targetClass, superMap);
                    }
                }
            }
            
            return Collections.emptyMap();
        }
    }
    
    private static Map<String, Type> processSuperGenericTypeArgument(
        final Type superType, final Class<?> superClass, final Map<String, Type> thisMap
    ) {
        if (superType instanceof ParameterizedType) {
            final Type[] actualTypeArguments = ((ParameterizedType) superType).getActualTypeArguments();
            final TypeVariable<?>[] typeParameters = superClass.getTypeParameters();
            
            final Map<String, Type> superMap = new HashMap<>();
            for (int i = 0; i < actualTypeArguments.length; i++) {
                final TypeVariable<?> par = typeParameters[i];
                final Type arg = actualTypeArguments[i];
                if ((arg instanceof Class) || arg instanceof ParameterizedType) {
                    superMap.put(par.getName(), arg);
                } else if (arg instanceof TypeVariable) {
                    Optional.ofNullable(thisMap.get(((TypeVariable<?>) arg).getName())).map(t ->
                        superMap.put(par.getName(), t)
                    );
                }
            }
            return superMap;
            
        } else {
            return Collections.emptyMap();
        }
    }
    
    public static <T> T[] toArray(final Class<?> type, final Collection<T> collection) {
        return collection.toArray(newArray(type, collection.size()));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(final T... items) {
        return items;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(final Class<?> type, final int size) {
        return (T[]) Array.newInstance(type, size);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Class<T> cast(final Class<?> clazz) {
        return (Class<T>) clazz;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T cast(final Object object) {
        return (T) object;
    }
    
    /**
     * @param source the source collection of subclass type
     * @param <T> the required supertype
     */
    @SuppressWarnings("unchecked")
    public static <T> Collection<T> castToSuperCollection(final Collection<? extends T> source) {
        return (Collection<T>) source;
    }
    
    /**
     * @param source the source collection of superclass type
     * @param <T> the required subtype
     */
    @SuppressWarnings("unchecked")
    public static <T> Collection<T> castToSubCollection(final Collection<? super T> source) {
        return (Collection<T>) source;
    }
    
    /**
     * @param source the source set of subclass type
     * @param <T> the required supertype
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> castToSuperSet(final Set<? extends T> source) {
        return (Set<T>) source;
    }
    
    /**
     * @param source the source set of superclass type
     * @param <T> the required subtype
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> castToSubSet(final Set<? super T> source) {
        return (Set<T>) source;
    }
    
    /**
     * @param source the source list of subclass type
     * @param <T> the required supertype
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> castToSuperList(final List<? extends T> source) {
        return (List<T>) source;
    }
    
    /**
     * @param source the source list of superclass type
     * @param <T> the required subtype
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> castToSubList(final List<? super T> source) {
        return (List<T>) source;
    }
    
    /**
     * @param source the source collection of subclass type
     * @param <K> the required key type
     * @param <V> the required value type
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> castToSuperMap(final Map<? extends K, ? extends V> source) {
        return (Map<K, V>) source;
    }
    
    /**
     * @param source the source collection of superclass type
     * @param <K> the required key type
     * @param <V> the required value type
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> castToSubMap(final Map<? super K, ? super V> source) {
        return (Map<K, V>) source;
    }
    
    private GenericsUtil() {}
    
}
