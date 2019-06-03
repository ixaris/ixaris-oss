package com.ixaris.commons.collections.lib;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class WeightedDirectedGraph<T, W> extends AbstractDirectedGraph<T, WeightedDirectedGraph<T, W>.V> {
    
    private final Weighting<W> weighting;
    
    public WeightedDirectedGraph(final Weighting<W> weighting) {
        
        this.weighting = weighting;
    }
    
    @Override
    protected final V createVertex(T vertex) {
        return new V(vertex);
    }
    
    public final void addEdge(final T parent, final T child, final W weight) {
        
        addEdge(addVertexInternal(parent), addVertexInternal(child), weight);
    }
    
    protected final void addEdge(final V parent, final V child, final W weight) {
        
        parent.addChild(child, weight);
        child.addParent(parent, weight);
        
        leaves.remove(parent);
        roots.remove(child);
    }
    
    public final Map<V, W> getWeightedParents(final T vertex) {
        return Collections.unmodifiableMap(getVertex(vertex).getWeightedParents());
    }
    
    public final Map<V, W> getWeightedChildren(final T vertex) {
        return Collections.unmodifiableMap(getVertex(vertex).getWeightedChildren());
    }
    
    /**
     * @param vertex
     * @param preference
     * @return
     */
    public final Map<T, W> traverseParents(final T vertex, final WeightingPreference preference) {
        return traverseParents(getVertex(vertex), preference);
    }
    
    private Map<T, W> traverseParents(final V vertex, final WeightingPreference preference) {
        return traverse(vertex, parentsCallback, preference);
    }
    
    /**
     * @param vertex
     * @param preference
     * @return
     */
    public final Map<T, W> traverseChildren(final T vertex, final WeightingPreference preference) {
        return traverseChildren(getVertex(vertex), preference);
    }
    
    private Map<T, W> traverseChildren(final V vertex, final WeightingPreference preference) {
        return traverse(vertex, childrenCallback, preference);
    }
    
    private final Map<V, W> empty = Collections.emptyMap();
    
    class V extends Vertex<T, V> {
        
        private Map<V, W> parents;
        private Map<V, W> children;
        
        public V(final T item) {
            super(item);
        }
        
        public boolean isRoot() {
            return parents == null;
        }
        
        public boolean isLeaf() {
            return children == null;
        }
        
        @Override
        Set<V> getParents() {
            return getWeightedParents().keySet();
        }
        
        Map<V, W> getWeightedParents() {
            if (parents == null) {
                return empty;
            } else {
                return parents;
            }
        }
        
        void addParent(final V parent, final W weight) {
            if (parents == null) {
                parents = new LinkedHashMap<>();
            }
            parents.put(parent, weight);
        }
        
        @Override
        void removeParent(final V parent) {
            if (parents != null) {
                parents.remove(parent);
                if (parents.isEmpty()) {
                    parents = null;
                }
            }
        }
        
        @Override
        Set<V> getChildren() {
            return getWeightedChildren().keySet();
        }
        
        Map<V, W> getWeightedChildren() {
            if (children == null) {
                return empty;
            } else {
                return children;
            }
        }
        
        protected void addChild(final V child, final W weight) {
            if (children == null) {
                children = new LinkedHashMap<>();
            }
            children.put(child, weight);
        }
        
        @Override
        protected void removeChild(final V child) {
            if (children != null) {
                children.remove(child);
                if (children.isEmpty()) {
                    children = null;
                }
            }
        }
    }
    
    public interface Weighting<T> extends Comparator<T> {
        
        T computeCombinedWeight(T w1, T w2);
        
        T getInitialWeight();
    }
    
    public static enum WeightingPreference {
        
        ANY, // any weight will do
        SMALLEST, // get smallest weight (will traverse all paths)
        LARGEST // get largest weight (will traverse all paths)
    
    }
    
    /**
     * Sort the graph in a breadth first way starting from the given vertex
     *
     * @param vertex
     * @param callback
     * @return
     */
    private Map<T, W> traverse(final V vertex, final Callback callback, final WeightingPreference preference) {
        
        final Map<T, W> ret = new HashMap<>();
        
        dfsVisit(vertex, weighting.getInitialWeight(), ret, new HashSet<>(), callback, preference);
        
        return ret;
    }
    
    private void dfsVisit(
        final V vertex,
        final W weight,
        final Map<T, W> ret,
        final Set<V> visited,
        final Callback callback,
        final WeightingPreference preference
    ) {
        
        visited.add(vertex);
        
        for (final Map.Entry<V, W> e : callback.getVertexSet(vertex).entrySet()) {
            
            final V v = e.getKey();
            final W combinedWeight = weighting.computeCombinedWeight(weight, e.getValue());
            final W existingWeight = ret.get(v.get());
            
            boolean visit;
            
            if (existingWeight == null) {
                visit = true;
            } else if (visited.contains(v)) {
                visit = false;
            } else {
                switch (preference) {
                    case LARGEST:
                        visit = weighting.compare(combinedWeight, existingWeight) > 0;
                        break;
                    case SMALLEST:
                        visit = weighting.compare(combinedWeight, existingWeight) < 0;
                        break;
                    default:
                        // do nothing and skip reprocessing this node
                        visit = false;
                        break;
                }
            }
            
            if (visit) {
                ret.put(v.get(), combinedWeight);
                dfsVisit(v, combinedWeight, ret, visited, callback, preference);
            }
        }
        
        visited.remove(vertex);
    }
    
    /**
     * Sort callback
     */
    private abstract class Callback {
        
        public abstract Map<V, W> getVertexSet(V v);
    }
    
    private final Callback parentsCallback = new Callback() {
        
        @Override
        public Map<V, W> getVertexSet(final V v) {
            return v.getWeightedParents();
        }
    };
    
    private final Callback childrenCallback = new Callback() {
        
        @Override
        public Map<V, W> getVertexSet(final V v) {
            return v.getWeightedChildren();
        }
    };
}
