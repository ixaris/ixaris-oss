package com.ixaris.commons.collections.lib;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractDirectedGraph<T, V extends Vertex<T, V>> {
    
    private final Map<T, V> vertices = new HashMap<>();
    protected final Set<V> roots = new LinkedHashSet<>();
    protected final Set<V> leaves = new LinkedHashSet<>();
    
    public AbstractDirectedGraph() {
        super();
    }
    
    public final Set<T> getValues() {
        return Collections.unmodifiableSet(vertices.keySet());
    }
    
    public final Set<T> getRoots() {
        return roots.stream().map(Vertex::get).collect(Collectors.toSet());
    }
    
    public final Set<T> getLeaves() {
        return leaves.stream().map(Vertex::get).collect(Collectors.toSet());
    }
    
    public final boolean contains(final T vertex) {
        return vertices.containsKey(vertex);
    }
    
    final V getVertex(final T vertex) {
        final V v = vertices.get(vertex);
        
        if (v == null) {
            throw new IllegalArgumentException("This graph does not contain vertex [" + vertex + "]");
        }
        
        return v;
    }
    
    /**
     * Adds vertex to DAG. If vertex for given vertex already exist then no vertex is added
     *
     * @param vertex Vhe vertex of the Vertex
     */
    public final void addVertex(final T vertex) {
        addVertexInternal(vertex);
    }
    
    final V addVertexInternal(final T vertex) {
        // check if vertex is alredy in DAG
        V v = vertices.get(vertex);
        if (v == null) {
            v = createVertex(vertex);
            vertices.put(vertex, v);
            roots.add(v);
            leaves.add(v);
        }
        
        return v;
    }
    
    protected abstract V createVertex(final T vertex);
    
    /**
     * Adds vertex to DAG. If vertex for given vertex already exist then no vertex is added
     *
     * @param vertex Vhe vertex of the Vertex
     */
    public final void removeVertex(final T vertex) {
        // check if vertex is already in DAG
        final V v = vertices.remove(vertex);
        if (v != null) {
            for (V parent : v.getParents()) {
                removeEdge(parent, v);
            }
            for (V child : v.getChildren()) {
                removeEdge(v, child);
            }
            
            roots.remove(v);
            leaves.remove(v);
        }
    }
    
    public final void removeEdge(final T parent, final T child) {
        removeEdge(addVertexInternal(parent), addVertexInternal(child));
    }
    
    final void removeEdge(final V parent, final V child) {
        parent.removeChild(child);
        if (parent.isLeaf()) {
            leaves.add(parent);
        }
        
        child.removeParent(parent);
        if (child.isRoot()) {
            roots.add(child);
        }
    }
    
    public final Set<T> getParents(final T vertex) {
        return convert(getVertex(vertex).getParents());
    }
    
    public final Set<T> getChildren(final T vertex) {
        return convert(getVertex(vertex).getChildren());
    }
    
    private Set<T> convert(final Set<V> vertices) {
        final HashSet<T> ret = new HashSet<>();
        for (V v : vertices) {
            ret.add(v.get());
        }
        return ret;
    }
    
    /**
     * Indicates if given vertex has no parent
     *
     * @return <code>true</true> if this vertex has no parent, <code>false</code> otherwise
     */
    public final boolean isRoot(final T vertex) {
        return getVertex(vertex).isRoot();
    }
    
    /**
     * Indicates if given vertex has no child
     *
     * @return <code>true</true> if this vertex has no child, <code>false</code> otherwise
     */
    public final boolean isLeaf(final T vertex) {
        return getVertex(vertex).isLeaf();
    }
    
    /**
     * Indicates if there is at least one edge leading to or from given vertex
     *
     * @return <code>true</true> if this vertex is connected with other vertex,<code>false</code> otherwise
     */
    public final boolean isConnected(final T vertex) {
        final V v = getVertex(vertex);
        return !v.isLeaf() || !v.isRoot();
    }
    
    public final int size() {
        return vertices.size();
    }
    
}
