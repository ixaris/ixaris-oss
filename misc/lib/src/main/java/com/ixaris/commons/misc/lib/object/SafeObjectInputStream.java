package com.ixaris.commons.misc.lib.object;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A safe object input stream that only allows objects of the specified types to be deserialized. Trying to deserialize
 * any object whose type is not white listed will throw an {@link InvalidClassException}.
 *
 * The following classes are whitelisted by default:
 * <ul>
 *     <li>{@link String}</li>
 *     <li>{@link Date}</li>
 *     <li>{@link LocalDate}</li>
 *     <li>{@link HashMap}</li>
 *     <li>{@link Map}</li>
 *     <li>{@link Enum}</li>
 * </ul>
 */
public class SafeObjectInputStream extends ObjectInputStream {
    
    /**
     * The set of white listed class names.
     */
    private final Set<String> allowedClasses;
    
    private final Set<Class<?>> alwaysAllowedClasses;
    
    /**
     * Create a new instance that wraps the given input stream, and only allows reading the specified classes.
     *
     * @param in the input stream to wrap
     * @param allowedClasses the set of types that are allowed to be deserialized
     * @throws IOException inherited from parent constructor
     */
    public SafeObjectInputStream(final InputStream in, final Class<?>... allowedClasses) throws IOException {
        super(in);
        this.allowedClasses = Arrays.stream(allowedClasses).map(Class::getName).collect(Collectors.toSet());
        
        alwaysAllowedClasses = new HashSet<>();
        setupAlwaysAllowedClasses();
    }
    
    private void setupAlwaysAllowedClasses() {
        alwaysAllowedClasses.add(String.class);
        alwaysAllowedClasses.add(Date.class);
        alwaysAllowedClasses.add(LocalDate.class);
        alwaysAllowedClasses.add(HashMap.class);
        alwaysAllowedClasses.add(Map.class);
        alwaysAllowedClasses.add(Enum.class);
    }
    
    @Override
    @SuppressWarnings("squid:S1067")
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        final Class<?> clazz = super.resolveClass(desc);
        
        if (clazz.isArray() || clazz.isPrimitive() || Number.class.isAssignableFrom(clazz) || allowedClasses.contains(desc.getName())
            || alwaysAllowedClasses.contains(clazz)) {
            return clazz;
        }
        
        throw new InvalidClassException(desc.getName(), "Class is not allowed to be deserialized");
    }
    
}
