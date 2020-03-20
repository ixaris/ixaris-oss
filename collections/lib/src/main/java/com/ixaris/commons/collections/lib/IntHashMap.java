package com.ixaris.commons.collections.lib;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class IntHashMap<T> implements IntMap<T> {
    
    private static final class Node<T> implements Entry<T> {
        
        private final int key;
        private T value;
        private Node<T> next;
        
        private Node(final int key, final T value, final Node<T> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
        
        @Override
        public int getKey() {
            return key;
        }
        
        @Override
        public T getValue() {
            return value;
        }
        
        @Override
        public T setValue(final T value) {
            final T oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        
    }
    
    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    
    private static int tableSizeFor(final int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : ((n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : (n + 1));
    }
    
    private Node<T>[] table;
    
    /**
     * The total number of entries in the hash table.
     */
    private int count;
    
    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     *
     * @serial
     */
    private int threshold;
    
    /**
     * The load factor for the hashtable.
     *
     * @serial
     */
    private final float loadFactor;
    
    /**
     * <p>Constructs a new, empty hashtable with a default capacity and load
     * factor, which is <code>20</code> and <code>0.75</code> respectively.</p>
     */
    public IntHashMap() {
        this(16, 0.75f);
    }
    
    /**
     * <p>Constructs a new, empty hashtable with the specified initial capacity
     * and default load factor, which is <code>0.75</code>.</p>
     *
     * @param initialCapacity the initial capacity of the hashtable.
     * @throws IllegalArgumentException if the initial capacity is less
     *     than zero.
     */
    public IntHashMap(final int initialCapacity) {
        this(initialCapacity, 0.75f);
    }
    
    /**
     * <p>Constructs a new, empty hashtable with the specified initial
     * capacity and the specified load factor.</p>
     *
     * @param initialCapacity the initial capacity of the hashtable.
     * @param loadFactor the load factor of the hashtable.
     * @throws IllegalArgumentException if the initial capacity is less
     *     than zero, or if the load factor is nonpositive.
     */
    @SuppressWarnings("unchecked")
    public IntHashMap(int initialCapacity, final float loadFactor) {
        super();
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
        
        this.loadFactor = loadFactor;
        threshold = tableSizeFor(initialCapacity);
        table = new Node[threshold];
    }
    
    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param m the map whose mappings are to be placed in this map
     * @throws NullPointerException if the specified map is null
     */
    public IntHashMap(final IntMap<? extends T> m) {
        this(m.size());
        final int size = m.size();
        if (size > 0) {
            for (final IntMap.Entry<? extends T> e : m) {
                put(e.getKey(), e.getValue());
            }
        }
    }
    
    @Override
    public int size() {
        return count;
    }
    
    @Override
    public boolean isEmpty() {
        return count == 0;
    }
    
    @Override
    public boolean containsValue(final T value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }
        
        for (Node<T> node : table) {
            for (; node != null; node = node.next) {
                if (node.value.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public boolean containsKey(final int key) {
        final int index = getIndex(key, table.length);
        for (Node<T> e = table[index]; e != null; e = e.next) {
            if (e.key == key) {
                return true;
            }
        }
        return false;
    }
    
    @SuppressWarnings("squid:S1171")
    @Override
    public Iterator<Entry<T>> iterator() {
        return new Iterator<Entry<T>>() {
            
            private int index = 0;
            private Node<T> cur;
            
            {
                findNext();
            }
            
            @Override
            public boolean hasNext() {
                return cur != null;
            }
            
            @Override
            public Entry<T> next() {
                final Node<T> next = this.cur;
                findNext();
                if (next == null) {
                    throw new NoSuchElementException();
                }
                return next;
            }
            
            private void findNext() {
                if (cur != null) {
                    cur = cur.next;
                }
                while ((cur == null) && (index < table.length)) {
                    cur = table[index];
                    index++;
                }
            }
            
        };
    }
    
    /**
     * <p>Returns the value to which the specified key is mapped in this map.</p>
     *
     * @param key a key in the hashtable.
     * @return the value to which the key is mapped in this hashtable;
     *     <code>null</code> if the key is not mapped to any value in
     *     this hashtable.
     * @see #put(int, Object)
     */
    @Override
    public T get(final int key) {
        final int index = getIndex(key, table.length);
        for (Node<T> node = table[index]; node != null; node = node.next) {
            if (node.key == key) {
                return node.value;
            }
        }
        return null;
    }
    
    /**
     * <p>Maps the specified <code>key</code> to the specified
     * <code>value</code> in this hashtable. The key cannot be
     * <code>null</code>. </p>
     *
     * <p>The value can be retrieved by calling the <code>get</code> method
     * with a key that is equal to the original key.</p>
     *
     * @param key the hashtable key.
     * @param value the value.
     * @return the previous value of the specified key in this hashtable,
     *     or <code>null</code> if it did not have one.
     * @throws NullPointerException if the key is <code>null</code>.
     * @see #get(int)
     */
    @Override
    public T put(final int key, final T value) {
        // Makes sure the key is not already in the hashtable.
        int index = getIndex(key, table.length);
        for (Node<T> e = table[index]; e != null; e = e.next) {
            if (e.key == key) {
                final T old = e.value;
                e.value = value;
                return old;
            }
        }
        
        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();
            index = getIndex(key, table.length);
        }
        
        // Creates the new entry.
        table[index] = new Node<>(key, value, table[index]);
        count++;
        return null;
    }
    
    /**
     * <p>Removes the key (and its corresponding value) from this
     * hashtable.</p>
     *
     * <p>This method does nothing if the key is not present in the
     * hashtable.</p>
     *
     * @param key the key that needs to be removed.
     * @return the value to which the key had been mapped in this hashtable,
     *     or <code>null</code> if the key did not have a mapping.
     */
    @Override
    public T remove(int key) {
        final int index = getIndex(key, table.length);
        for (Node<T> e = table[index], prev = null; e != null; prev = e, e = e.next) {
            if (e.key == key) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    table[index] = e.next;
                }
                count--;
                final T oldValue = e.value;
                e.value = null;
                return oldValue;
            }
        }
        return null;
    }
    
    /**
     * <p>Clears this hashtable so that it contains no keys.</p>
     */
    @Override
    public void clear() {
        Arrays.fill(table, null);
        count = 0;
    }
    
    private int getIndex(int key, int length) {
        return (key & 0x7FFFFFFF) % length;
    }
    
    /**
     * <p>Increases the capacity of and internally reorganizes this
     * hashtable, in order to accommodate and access its entries more
     * efficiently.</p>
     *
     * <p>This method is called automatically when the number of keys
     * in the hashtable exceeds this hashtable's capacity and load
     * factor.</p>
     */
    @SuppressWarnings({ "unchecked", "squid:S2164" })
    private void rehash() {
        final Node<T>[] oldMap = table;
        final int newCapacity = oldMap.length * 2;
        threshold = (int) (newCapacity * loadFactor);
        final Node<T>[] newMap = new Node[newCapacity];
        
        table = newMap;
        
        for (Node<T> node : oldMap) {
            for (; node != null; node = node.next) {
                final int index = getIndex(node.key, newCapacity);
                node.next = newMap[index];
                newMap[index] = node;
            }
        }
    }
    
}
