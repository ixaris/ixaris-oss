package com.ixaris.commons.collections.lib;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Various utility methods to simplify working with sets.
 *
 * @author daniel.grech
 */
public final class SetUtils {
    
    /**
     * Given a set s and an integer k, this method generates all <strong>combinations</strong> of size k from s. Note that this generates
     * combinations, not permutations (i.e. {a,b} = {b,a}). This method works in terms of sets, thus there will be no duplicates and no ordering
     * on the resulting set.
     *
     * <p>This method works by taking the first element out of the set, and then generating all the combinations of k-1 from the resulting
     * subset. Then, we add the first element to all of the generated combinations. Next, we generate all the combinations of size k from the
     * resulting subset. Thus, there are two recursive calls. The base cases are:
     *
     * <ul>
     *   <li>when S = {} - returns the empty set
     *   <li>when k = 0 - returns the empty set
     *   <li>when k = 1 - returns the elements of that set wrapped as one-element sets
     * </ul>
     *
     * <p>Examples:
     *
     * <ul>
     *   <li><code>combinations({}, 2) = {}</code>
     *   <li><code>combinations({a,b,c}, 0) = {}</code>
     *   <li><code>combinations({a,b,c}, 1) = {{a},{b},{c}}</code>
     *   <li><code>combinations({a,b,c}, 2) = {{a,b},{a,c},{b,c}}</code>
     *   <li><code>combinations({a,b,c,d}, 3) = {{a,b,c},{a,b,d},{a,c,d},{b,c,d}}</code>
     * </ul>
     *
     * @param s the set from which to extract combinations
     * @param k the size of the combinations
     * @param <T> the type of the set's elements
     * @throws IllegalArgumentException if s is null
     * @throws IllegalArgumentException if k is negative
     * @return a set containing combination sets
     */
    public static <T> Set<Set<T>> combinations(final Set<T> s, final int k) {
        
        if (s == null) {
            throw new IllegalArgumentException("set is null");
        }
        
        if (k < 0) {
            throw new IllegalArgumentException("k must be >= 0");
        }
        
        // The zero-element combinations of any set is the empty set; i.e. combinations(S,0) = {}
        // The k-element combinations of the empty set is the empty set; i.e. combinations({},k) = {}
        if (s.isEmpty() || k == 0) {
            return Collections.emptySet();
        }
        
        // The 1-element combinations of any set is the elements of that set, i.e. combinations({a,b},1) = {{a},{b}}
        if (k == 1) {
            final Set<Set<T>> wrapped = new HashSet<>();
            for (final T t : s) {
                wrapped.add(Collections.singleton(t));
            }
            return wrapped;
        }
        
        final Set<Set<T>> combinations = new HashSet<>();
        
        // Split the set: take the first element out and leave the rest as a subset
        final T first = s.iterator().next();
        final Set<T> subSet = new HashSet<>(s);
        subSet.remove(first);
        
        // Generate all the k-1 combinations of the subset and add the first element to them
        final Set<Set<T>> subsetCombinations = combinations(subSet, k - 1);
        for (Set<T> set : subsetCombinations) {
            final Set<T> newSet = new HashSet<>(set);
            newSet.add(first);
            combinations.add(newSet);
        }
        
        // Generate all the k combinations of the subset
        combinations.addAll(combinations(subSet, k));
        
        return combinations;
    }
    
    private SetUtils() {}
    
}
