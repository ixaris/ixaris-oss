package com.ixaris.commons.collections.lib;

import com.ixaris.commons.misc.lib.object.Tuple2;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Multimap interface. This map can contain multiple values for a key
 *
 * @author brian.vella
 * @param <K>
 * @param <V>
 */
public interface MultiMap<K, V> extends Serializable, Iterable<Entry<K, ? extends Collection<V>>> {
    
    /**
     * Returns the number of key mappings in this map. If the map contains more than <tt>Integer.MAX_VALUE</tt> kays,
     * returns <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key mappings in this map
     */
    int size();
    
    /**
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    boolean isEmpty();
    
    /**
     * @param key key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key
     */
    boolean containsKey(K key);
    
    /**
     * @param key key whose presence in this map is to be tested
     * @param value value whise presents in this map is to be tested
     * @return true if this map contains a mapping for the specified key and value
     */
    boolean containsEntry(K key, V value);
    
    /**
     * @param key - key to search for in multimap
     * @return the collection of values that the key maps to
     */
    Collection<V> get(K key);
    
    // Modification Operations
    
    /**
     * @param key - key to store in the multimap
     * @param value - value to store in the multimap
     * @return true if the method increased the size of the multimap, or false if the multimap already contained the
     *     key-value pair and doesn't allow duplicates
     */
    boolean put(K key, V value);
    
    /**
     * @param key - key of entry to remove from the multimap
     * @param value - value of entry to remove the multimap
     * @return true if the multimap changed
     */
    boolean remove(K key, V value);
    
    // Bulk Operations
    
    /**
     * Removes all values associated with a given key.
     *
     * @param key - key of entries to remove from the multimap
     * @return the collection of removed values, or an empty collection if no values were associated with the provided
     *     key.
     */
    Collection<V> remove(K key);
    
    /**
     * @param key - key to store in the multimap
     * @param values - values to store in the multimap
     * @return true if the multimap changed
     */
    boolean putAll(K key, Collection<V> values);
    
    /**
     * @param key - key to store in the multimap
     * @param values - values to store in the multimap
     * @return true if the multimap changed
     */
    boolean putAll(K key, Iterable<? extends V> values);
    
    /**
     * @param map - mappings to store in this multimap
     * @return true if the multimap changed
     */
    boolean putAll(MultiMap<? extends K, ? extends V> map);
    
    /**
     * @param collection - collection to store in this multimap
     * @return true if the multimap changed
     */
    boolean putAll(Collection<? extends Tuple2<? extends K, ? extends V>> collection);
    
    /**
     * @param key - key to store in the multimap
     * @param values - values to store in the multimap
     * @return the collection of replaced values, or an empty collection if no values were previously associated with
     *     the key.
     */
    Collection<V> replace(K key, Collection<V> values);
    
    /**
     * @param key - key to store in the multimap
     * @param values - values to store in the multimap
     * @return the collection of replaced values, or an empty collection if no values were previously associated with
     *     the key.
     */
    Collection<V> replace(K key, Iterable<? extends V> values);
    
    /**
     * @param key - key to store in the multimap
     * @param values - values to remove in the multimap
     * @return true if the multimap changed
     */
    boolean removeAll(K key, Collection<V> values);
    
    /**
     * @param key - key to store in the multimap
     * @param values - values to remove in the multimap
     * @return true if the multimap changed
     */
    boolean removeAll(K key, Iterable<? extends V> values);
    
    /**
     * @param map - mappings to store in this multimap
     * @return true if the multimap changed
     */
    boolean removeAll(MultiMap<? extends K, ? extends V> map);
    
    /**
     * @param collection - collection to store in this multimap
     * @return true if the multimap changed
     */
    boolean removeAll(Collection<? extends Tuple2<? extends K, ? extends V>> collection);
    
    /**
     * Clear contents
     */
    void clear();
    
    // Views
    
    /**
     * @return a read only view of this multimap's keys
     */
    Set<K> keySet();
    
    /**
     * @return a read only view of this multimap as a map of key to collection of values
     */
    Map<K, ? extends Collection<V>> asMap();
    
    /**
     * @param updates to apply
     */
    void apply(MultiMapUpdates<K, V> updates);
}
