package com.ixaris.commons.collections.lib;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ixaris.commons.collections.lib.DirectedAcyclicGraph.CycleDetectedException;

public class DirectedAcyclicGraphTest {
    
    @Test
    public void testCycleGraph() throws CycleDetectedException {
        final DirectedAcyclicGraph<String> graph = new DirectedAcyclicGraph<String>();
        
        graph.addVertex("1");
        graph.addVertex("2");
        graph.addVertex("3");
        graph.addVertex("4");
        graph.addVertex("5");
        
        graph.addEdge("1", "2");
        graph.addEdge("2", "3");
        graph.addEdge("3", "4");
        graph.addEdge("4", "5");
        
        try {
            graph.addEdge("5", "2");
        } catch (CycleDetectedException e) {
            assertEquals("Relationship between parent [5] and child [2] introduces a cycle [2 -> 5 -> 4 -> 3 -> 2] in the graph", e.getMessage());
        }
    }
    
}
