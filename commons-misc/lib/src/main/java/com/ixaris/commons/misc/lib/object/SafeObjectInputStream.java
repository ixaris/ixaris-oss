package com.ixaris.commons.misc.lib.object;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A safe object input stream that only allows objects of the specified types to be deserialized. Trying to deserialize
 * any object whose type is not white listed will throw an {@link InvalidClassException}.
 */
public class SafeObjectInputStream extends ObjectInputStream {
    
    /**
     * The set of white listed class names.
     */
    private final Set<String> allowedClasses;
    
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
    }
    
    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        if (allowedClasses.contains(desc.getName())) {
            return super.resolveClass(desc);
        }
        
        throw new InvalidClassException(desc.getName(), "Class is not allowed to be deserialized");
    }
}
