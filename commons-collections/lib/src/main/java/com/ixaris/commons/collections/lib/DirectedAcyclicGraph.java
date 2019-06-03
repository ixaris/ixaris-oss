package com.ixaris.commons.collections.lib;

import static com.ixaris.commons.collections.lib.VertexState.VISITED;
import static com.ixaris.commons.collections.lib.VertexState.VISITING;
import static com.ixaris.commons.collections.lib.VertexState.isInState;
import static com.ixaris.commons.collections.lib.VertexState.isNotProcessed;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Unweighted directed acyclic graph
 *
 * @author <a href="mailto:brian.vella@ixaris.com">brian.vella</a>
 */
public class DirectedAcyclicGraph<T> extends DirectedGraph<T> {
    
    public DirectedAcyclicGraph() {
        super();
    }
    
    @Override
    protected final void addEdge(final V parent, final V child) {
        
        super.addEdge(parent, child);
        
        final List<V> cycle = findCycleFromVertex(child);
        
        if (cycle != null) {
            removeEdge(parent, child);
            throw new CycleDetectedException(parent, child, cycle);
        }
    }
    
    /**
     * Detect if a cycle was introduced when adding an edge to this vertex
     *
     * @param vertex
     * @return
     */
    private List<V> findCycleFromVertex(final V vertex) {
        
        final LinkedList<V> cycle = new LinkedList<>();
        
        final boolean hasCycle = dfsVisit(vertex, cycle, new HashMap<>());
        
        if (hasCycle) {
            /* we have a situation like: [a, b, c, d, e, f, g, d].
             * Vertex which introduced the cycle is the last on the stack
             * We have to find the first occurrence of this Vertex and
             * use its position to get the actual cycle
             * which in this case would be [d, e, f, g, d] */
            
            final V top = cycle.peek();
            final int pos = cycle.indexOf(top);
            
            final List<V> ret = cycle.subList(pos, cycle.size());
            Collections.reverse(ret);
            return ret;
        }
        
        return null;
    }
    
    private boolean dfsVisit(final V vertex, final Deque<V> cycle, final Map<V, VertexState> vertexStateMap) {
        
        cycle.push(vertex);
        vertexStateMap.put(vertex, VISITING);
        
        for (final V v : vertex.getParents()) {
            
            if (isNotProcessed(v, vertexStateMap)) {
                
                if (dfsVisit(v, cycle, vertexStateMap)) {
                    return true;
                }
            } else if (isInState(v, VISITING, vertexStateMap)) {
                
                cycle.push(v);
                return true;
            }
        }
        
        vertexStateMap.put(vertex, VISITED);
        cycle.pop();
        return false;
    }
    
    public static final class CycleDetectedException extends RuntimeException {
        
        private static final long serialVersionUID = -1560299898354422411L;
        
        private final List<?> cycle;
        
        public <T> CycleDetectedException(final T parent, final T child, final List<T> cycle) {
            super(String.format(
                "Relationship between parent [%s] and child [%s] introduces a cycle [%s] in the graph",
                parent, child, cycleToString(cycle)
            ));
            this.cycle = cycle; // NOSONAR only used internally to this class
        }
        
        public List<?> getCycle() {
            return Collections.unmodifiableList(cycle);
        }
        
        private static <T> String cycleToString(final List<T> cycle) {
            final StringBuilder sb = new StringBuilder();
            
            boolean first = true;
            for (T v : cycle) {
                if (first) {
                    first = false;
                } else {
                    sb.append(" -> ");
                }
                sb.append(v);
            }
            return sb.toString();
        }
    }
    
}
