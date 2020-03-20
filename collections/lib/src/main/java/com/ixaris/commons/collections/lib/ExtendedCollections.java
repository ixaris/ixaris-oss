package com.ixaris.commons.collections.lib;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.ixaris.commons.misc.lib.object.GenericsUtil;

/**
 * Extends java.util.Collections with extras like colections of collections and multimaps
 *
 * @author brian.vella
 */
public final class ExtendedCollections {
    
    public static <T, C extends Collection<T>> C build(final C collection, final T... values) {
        if ((values != null) && values.length > 0) {
            Collections.addAll(collection, values);
        }
        return collection;
    }
    
    public static <K, V, M extends Map<K, V>> M buildMap(final M map, final K k1, final V v1) {
        map.put(k1, v1);
        return map;
    }
    
    public static <K, V, M extends Map<K, V>> M buildMap(final M map, final K k1, final V v1, final K k2, final V v2) {
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
    
    public static <K, V, M extends Map<K, V>> M buildMap(final M map, final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
    
    public static <K, V, M extends Map<K, V>> M buildMap(final M map,
                                                         final K k1,
                                                         final V v1,
                                                         final K k2,
                                                         final V v2,
                                                         final K k3,
                                                         final V v3,
                                                         final K k4,
                                                         final V v4) {
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }
    
    public static <K, V, M extends Map<K, V>> M buildMap(final M map,
                                                         final K k1,
                                                         final V v1,
                                                         final K k2,
                                                         final V v2,
                                                         final K k3,
                                                         final V v3,
                                                         final K k4,
                                                         final V v4,
                                                         final K k5,
                                                         final V v5) {
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }
    
    public static <K, V, M extends Map<K, V>> M buildMap(final M map,
                                                         final K k1,
                                                         final V v1,
                                                         final K k2,
                                                         final V v2,
                                                         final K k3,
                                                         final V v3,
                                                         final K k4,
                                                         final V v4,
                                                         final K k5,
                                                         final V v5,
                                                         final K k6,
                                                         final V v6) {
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        return map;
    }
    
    public static <T> Collection<Collection<T>> unmodifiableCollectionOfCollection(final Collection<? extends Collection<T>> c) {
        return new UnmodifiableCollectionOfCollection<T>(c);
    }
    
    public static <K, V> Map<K, Collection<V>> unmodifiableMapOfCollection(final Map<K, ? extends Collection<V>> m) {
        return new UnmodifiableMapOfCollection<>(m);
    }
    
    public static <T> Iterable<T> toIterable(final Enumeration<T> e) {
        return () -> new Iterator<T>() {
            
            @Override
            public boolean hasNext() {
                return e.hasMoreElements();
            }
            
            @Override
            public T next() {
                return e.nextElement();
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    
    public static <T> Enumeration<T> toEnumeration(final Iterable<T> i) {
        return toEnumeration(i.iterator());
    }
    
    public static <T> Enumeration<T> toEnumeration(final Iterator<T> i) {
        return new Enumeration<T>() {
            
            @Override
            public boolean hasMoreElements() {
                return i.hasNext();
            }
            
            @Override
            public T nextElement() {
                return i.next();
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    public static <T, U> U[] mapArray(final T[] source, final Function<T, U> function) {
        final U[] target = (U[]) new Object[source.length];
        for (int i = 0; i < source.length; i++) {
            target[i] = function.apply(source[i]);
        }
        return target;
    }
    
    private static class UnmodifiableCollection<E> implements Collection<E>, Serializable {
        
        private static final long serialVersionUID = 1820017752578914078L;
        
        final Collection<? extends E> collection;
        
        private UnmodifiableCollection(final Collection<? extends E> collection) {
            if (collection == null) {
                throw new IllegalArgumentException("collection is null");
            }
            this.collection = collection;
        }
        
        @Override
        public int size() {
            return collection.size();
        }
        
        @Override
        public boolean isEmpty() {
            return collection.isEmpty();
        }
        
        @Override
        public boolean contains(final Object o) {
            return collection.contains(o);
        }
        
        @Override
        public Object[] toArray() {
            return collection.toArray();
        }
        
        @Override
        public <T> T[] toArray(final T[] a) {
            return collection.toArray(a);
        }
        
        @Override
        public String toString() {
            return collection.toString();
        }
        
        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                
                Iterator<? extends E> i = collection.iterator();
                
                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }
                
                @Override
                public E next() {
                    return i.next();
                }
                
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                
            };
        }
        
        @Override
        public boolean add(final E e) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean remove(final Object o) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean containsAll(final Collection<?> coll) {
            return collection.containsAll(coll);
        }
        
        @Override
        public boolean addAll(final Collection<? extends E> coll) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean removeAll(final Collection<?> coll) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean retainAll(final Collection<?> coll) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
        
    }
    
    private static class UnmodifiableSet<E> extends UnmodifiableCollection<E> implements Set<E>, Serializable {
        
        private static final long serialVersionUID = -9215047833775013803L;
        
        private UnmodifiableSet(Set<? extends E> s) {
            super(s);
        }
        
        @Override
        public boolean equals(final Object o) {
            return (o == this) || collection.equals(o);
        }
        
        @Override
        public int hashCode() {
            return collection.hashCode();
        }
    }
    
    private static class UnmodifiableCollectionOfCollection<E> extends UnmodifiableCollection<Collection<E>> {
        
        private static final long serialVersionUID = 1820017752578914078L;
        
        public UnmodifiableCollectionOfCollection(final Collection<? extends Collection<E>> c) {
            super(c);
        }
        
        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public <T> T[] toArray(final T[] a) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public Iterator<Collection<E>> iterator() {
            return new Iterator<Collection<E>>() {
                
                Iterator<? extends Collection<E>> i = collection.iterator();
                
                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }
                
                @Override
                public Collection<E> next() {
                    return Collections.unmodifiableCollection(i.next());
                }
                
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                
            };
        }
    }
    
    private static class UnmodifiableMapOfCollection<K, V> implements Map<K, Collection<V>>, Serializable {
        
        private static final long serialVersionUID = -1034234728574286014L;
        
        private final Map<K, ? extends Collection<V>> map;
        
        private UnmodifiableMapOfCollection(final Map<K, ? extends Collection<V>> map) {
            if (map == null) {
                throw new IllegalArgumentException("map is null");
            }
            this.map = map;
        }
        
        @Override
        public int size() {
            return map.size();
        }
        
        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }
        
        @Override
        public boolean containsKey(final Object key) {
            return map.containsKey(key);
        }
        
        @Override
        public boolean containsValue(final Object val) {
            return map.containsValue(val);
        }
        
        @Override
        public Collection<V> get(final Object key) {
            final Collection<V> v = map.get(key);
            return v != null ? Collections.unmodifiableCollection(v) : null;
        }
        
        @Override
        public Collection<V> put(final K key, final Collection<V> value) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public Collection<V> remove(final Object key) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void putAll(final Map<? extends K, ? extends Collection<V>> m) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
        
        private transient Set<K> keySet = null;
        private transient Set<Entry<K, Collection<V>>> entrySet = null;
        private transient Collection<Collection<V>> values = null;
        
        @Override
        public Set<K> keySet() {
            if (keySet == null) {
                keySet = Collections.unmodifiableSet(map.keySet());
            }
            return keySet;
        }
        
        @Override
        public Set<Entry<K, Collection<V>>> entrySet() {
            if (entrySet == null) {
                entrySet = new UnmodifiableEntrySet<>(GenericsUtil.<Set<Entry<K, Collection<V>>>>cast(map.entrySet()));
            }
            return entrySet;
        }
        
        @Override
        public Collection<Collection<V>> values() {
            if (values == null) {
                values = unmodifiableCollectionOfCollection(map.values());
            }
            return values;
        }
        
        @Override
        public boolean equals(final Object o) {
            return o == this || map.equals(o);
        }
        
        @Override
        public int hashCode() {
            return map.hashCode();
        }
        
        @Override
        public String toString() {
            return map.toString();
        }
        
        private static class UnmodifiableEntrySet<K, V> extends UnmodifiableSet<Entry<K, Collection<V>>> {
            
            private static final long serialVersionUID = 7854390611657943733L;
            
            UnmodifiableEntrySet(final Set<? extends Entry<K, Collection<V>>> s) {
                super(s);
            }
            
            @Override
            public Iterator<Entry<K, Collection<V>>> iterator() {
                return new Iterator<Entry<K, Collection<V>>>() {
                    
                    Iterator<? extends Entry<K, Collection<V>>> i = collection.iterator();
                    
                    @Override
                    public boolean hasNext() {
                        return i.hasNext();
                    }
                    
                    @Override
                    public Entry<K, Collection<V>> next() {
                        return new UnmodifiableEntry<K, V>(i.next());
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                    
                };
            }
            
            @Override
            public Object[] toArray() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public <T> T[] toArray(T[] a) {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public boolean contains(final Object o) {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public boolean containsAll(final Collection<?> coll) {
                throw new UnsupportedOperationException();
            }
            
            private static class UnmodifiableEntry<K, V> implements Entry<K, Collection<V>> {
                
                private Entry<K, Collection<V>> e;
                
                UnmodifiableEntry(final Entry<K, Collection<V>> e) {
                    this.e = e;
                }
                
                @Override
                public K getKey() {
                    return e.getKey();
                }
                
                @Override
                public Collection<V> getValue() {
                    return Collections.unmodifiableCollection(e.getValue());
                }
                
                @Override
                public Collection<V> setValue(final Collection<V> value) {
                    throw new UnsupportedOperationException();
                }
                
                @Override
                public int hashCode() {
                    return e.hashCode();
                }
                
                @Override
                public String toString() {
                    return e.toString();
                }
                
            }
        }
    }
    
    private ExtendedCollections() {}
    
}
