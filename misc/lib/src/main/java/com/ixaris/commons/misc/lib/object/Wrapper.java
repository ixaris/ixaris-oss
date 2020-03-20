package com.ixaris.commons.misc.lib.object;

/**
 * Marker interface for wrappers. This allows the querying of wrappers of an object irrespective of the layers of wrapping, provided all wrappers
 * implement this interface.
 *
 * @param <T>
 */
public interface Wrapper<T> {
    
    /**
     * Fully unwrap an object by stripping all layers of wrappers.
     *
     * @param o
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    static <T> T unwrap(final T o) {
        T tmp = o;
        while (isWrapped(tmp)) {
            tmp = ((Wrapper<T>) tmp).unwrap();
        }
        return tmp;
    }
    
    static boolean isWrapped(final Object o) {
        return (o instanceof Wrapper);
    }
    
    /**
     * Iteratively checks all the wrappers. If there is one wrapper, this is equivalent to an instanceof check, but for multiple nested wrappers,
     * instanceof can only check the outermost wrapper, whereas this method goes through all wrapper layers that implement this interface.
     *
     * @param o
     * @param wrapperClass
     * @return
     */
    @SuppressWarnings("unchecked")
    static boolean isWrappedBy(final Object o, final Class<? extends Wrapper> wrapperClass) {
        Object tmp = o;
        while (isWrapped(tmp)) {
            if (wrapperClass.isAssignableFrom(tmp.getClass())) {
                return true;
            }
            tmp = ((Wrapper<?>) tmp).unwrap();
        }
        return false;
    }
    
    /**
     * @return the instance unwrapped from this wrapper
     */
    T unwrap();
    
}
