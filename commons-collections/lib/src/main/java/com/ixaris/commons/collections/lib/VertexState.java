package com.ixaris.commons.collections.lib;

import java.util.Map;

enum VertexState {
    
    VISITING,
    VISITED,
    POSTPONED;
    
    static <V> boolean isNotProcessed(final V vertex, final Map<V, VertexState> map) {
        
        return !map.containsKey(vertex);
    }
    
    static <V> boolean isInState(final V vertex, final VertexState expectedState, final Map<V, VertexState> map) {
        
        final VertexState state = map.get(vertex);
        return (state != null) && expectedState.equals(state);
    }
    
}
