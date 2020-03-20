package com.ixaris.commons.collections.lib;

import static com.ixaris.commons.collections.lib.VertexState.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Unweighted directed graph
 */
public class DirectedGraph<T> extends AbstractDirectedGraph<T, DirectedGraph<T>.V> {
    
    public DirectedGraph() {
        super();
    }
    
    @Override
    final V createVertex(final T vertex) {
        return new V(vertex);
    }
    
    public final void addEdge(final T parent, final T child) {
        addEdge(addVertexInternal(parent), addVertexInternal(child));
    }
    
    protected void addEdge(final V parent, final V child) {
        parent.addChild(child);
        child.addParent(parent);
        
        getLeavesInternal().remove(parent);
        getRootsInternal().remove(child);
    }
    
    /**
     * Sort the parents by proximity, or, distance from this node. Every node is assumed to have the same weight. Parents having the same
     * distance are sorted by insertion time, i.e. the first inserted is the first chosen
     *
     * @param vertex
     * @param reverse whether the returned list should be reversed
     * @return the sorted list of parents, from the closest to the farthest
     */
    public final List<T> getSortedParents(final T vertex, final boolean reverse) {
        return getSortedParents(getVertex(vertex), reverse);
    }
    
    private List<T> getSortedParents(final V vertex, final boolean reverse) {
        final List<T> sorted = sort(vertex, parentsCallback);
        
        if (reverse) {
            Collections.reverse(sorted);
        }
        
        return sorted;
    }
    
    /**
     * Sort the children by proximity, or, distance from this node. Every node is assumed to have the same weight. Parents having the same
     * distance are sorted by insertion time, i.e. the first inserted is the first chosen
     *
     * @param vertex
     * @param reverse whether the returned list should be reversed
     * @return the sorted list of children, from the closest to the farthest
     */
    public final List<T> getSortedChildren(final T vertex, final boolean reverse) {
        return getSortedChildren(getVertex(vertex), reverse);
    }
    
    private List<T> getSortedChildren(final V vertex, final boolean reverse) {
        final List<T> sorted = sort(vertex, childrenCallback);
        
        if (reverse) {
            Collections.reverse(sorted);
        }
        
        return sorted;
    }
    
    public final List<T> sortRootsToLeaves() {
        return sort(childrenCallback);
    }
    
    public final List<T> sortLeavesToRoots() {
        return sort(parentsCallback);
    }
    
    private final Set<V> empty = Collections.emptySet();
    
    public final class V extends Vertex<T, V> {
        
        private Set<V> parents;
        private Set<V> children;
        
        V(final T item) {
            super(item);
        }
        
        @Override
        public boolean isRoot() {
            return parents == null;
        }
        
        @Override
        public boolean isLeaf() {
            return children == null;
        }
        
        @Override
        Set<V> getParents() {
            if (parents == null) {
                return empty;
            } else {
                return parents;
            }
        }
        
        void addParent(final V parent) {
            if (parents == null) {
                parents = new LinkedHashSet<V>();
            }
            parents.add(parent);
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
            if (children == null) {
                return empty;
            } else {
                return children;
            }
        }
        
        void addChild(final V child) {
            if (children == null) {
                children = new LinkedHashSet<V>();
            }
            children.add(child);
        }
        
        @Override
        void removeChild(final V child) {
            if (children != null) {
                children.remove(child);
                if (children.isEmpty()) {
                    children = null;
                }
            }
        }
    }
    
    /**
     * Sort the whole graph in a breadth first way
     */
    private List<T> sort(final Callback callback) {
        final List<T> ret = new ArrayList<T>();
        final Map<V, VertexState> vertexStateMap = new HashMap<V, VertexState>();
        
        for (final V v : callback.getStartSet()) {
            if (isNotProcessed(v, vertexStateMap)) {
                bfsVisit(v, vertexStateMap, ret, callback);
            }
        }
        
        return ret;
    }
    
    private void bfsVisit(final V vertex, final Map<V, VertexState> vertexStateMap, final List<T> ret, final Callback callback) {
        vertexStateMap.put(vertex, VISITING);
        
        // we skip this vertex if one of it's reverse relations is being visited
        // (which means that we will eventually get to this vertex through a longer route)
        for (final V v : callback.getReverseVertexSet(vertex)) {
            if (isInState(v, VISITING, vertexStateMap) || isInState(v, POSTPONED, vertexStateMap)) {
                vertexStateMap.put(vertex, POSTPONED);
                return;
            } else if (isNotProcessed(v, vertexStateMap)) {
                bfsVisit(v, vertexStateMap, ret, callback);
                if (isInState(v, POSTPONED, vertexStateMap)) {
                    vertexStateMap.put(vertex, POSTPONED);
                    return;
                }
            }
        }
        
        ret.add(vertex.get());
        vertexStateMap.put(vertex, VISITED);
        
        for (final V v : callback.getVertexSet(vertex)) {
            if (isNotProcessed(v, vertexStateMap) || isInState(v, POSTPONED, vertexStateMap)) {
                bfsVisit(v, vertexStateMap, ret, callback);
            }
        }
    }
    
    /**
     * Sort the graph in a breadth first way starting from the given vertex
     *
     * @param vertex
     * @param callback
     * @return
     */
    private List<T> sort(final V vertex, final Callback callback) {
        final List<T> ret = new ArrayList<T>();
        final Map<V, VertexState> vertexStateMap = new HashMap<V, VertexState>();
        final Queue<V> queue = new LinkedHashSetQueue<V>();
        
        queue.offer(vertex); // NOSONAR no capacity restrictions
        
        while (!queue.isEmpty()) {
            // get the head of the queue and add it to the final list
            final V head = queue.poll();
            vertexStateMap.put(head, VISITING);
            
            ret.add(head.get());
            
            for (final V v : callback.getVertexSet(head)) {
                if (isNotProcessed(v, vertexStateMap)) {
                    vertexStateMap.put(v, VISITING);
                    queue.offer(v); // NOSONAR no capacity restrictions
                }
            }
            
            vertexStateMap.put(head, VISITED);
        }
        
        ret.remove(0);
        return ret;
    }
    
    /**
     * Sort callback
     */
    private abstract class Callback {
        
        public abstract Set<V> getStartSet();
        
        public abstract Set<V> getVertexSet(V v);
        
        public abstract Set<V> getReverseVertexSet(V v);
        
    }
    
    private final Callback parentsCallback = new Callback() {
        
        @Override
        public Set<V> getStartSet() {
            return getLeavesInternal();
        }
        
        @Override
        public Set<V> getVertexSet(final V v) {
            return v.getParents();
        }
        
        @Override
        public Set<V> getReverseVertexSet(final V v) {
            return v.getChildren();
        }
    };
    
    private final Callback childrenCallback = new Callback() {
        
        @Override
        public Set<V> getStartSet() {
            return getRootsInternal();
        }
        
        @Override
        public Set<V> getVertexSet(final V v) {
            return v.getChildren();
        }
        
        @Override
        public Set<V> getReverseVertexSet(final V v) {
            return v.getParents();
        }
        
    };
    
}
