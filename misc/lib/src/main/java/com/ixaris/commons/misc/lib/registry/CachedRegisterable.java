package com.ixaris.commons.misc.lib.registry;

public class CachedRegisterable<T extends Registerable> {
    
    public static <X extends Registerable> CachedRegisterable<X> in(final Registry<? super X> registry) {
        return new CachedRegisterable<X>(registry);
    }
    
    private final Registry<? super T> registry;
    private T reference;
    
    public CachedRegisterable(final Registry<? super T> registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry is null");
        }
        this.registry = registry;
    }
    
    /**
     * Assumes that if the key changed, set was called. As such, does not check if the held reference corresponds to the given key.
     *
     * @param key may be null
     * @return
     */
    @SuppressWarnings("unchecked")
    public T get(final String key) {
        if (key == null) {
            return null;
        }
        if (reference == null) {
            reference = (T) registry.resolve(key);
        }
        
        return reference;
    }
    
    /**
     * Call this to set a new key
     *
     * @param registerable
     * @return
     */
    public String set(final T registerable) {
        if (registerable == null) {
            this.reference = null;
            return null;
        } else {
            this.reference = registerable;
            return registerable.getKey();
        }
    }
    
}
