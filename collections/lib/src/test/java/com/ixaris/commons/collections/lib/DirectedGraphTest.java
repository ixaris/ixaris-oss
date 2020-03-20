package com.ixaris.commons.collections.lib;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class DirectedGraphTest {
    
    @Test
    public void testSimpleGraph() {
        DirectedGraph<String> graph = new DirectedGraph<String>();
        
        graph.addVertex("1");
        graph.addVertex("2");
        graph.addVertex("3");
        graph.addVertex("4");
        graph.addVertex("5");
        
        graph.addEdge("1", "2");
        graph.addEdge("3", "2");
        graph.addEdge("2", "4");
        graph.addEdge("3", "4");
        graph.addEdge("4", "5");
        
        assertEquals(5, graph.size());
        assertTrue(graph.isRoot("1"));
        assertFalse(graph.isLeaf("1"));
        assertFalse(graph.isRoot("2"));
        assertFalse(graph.isLeaf("2"));
        assertTrue(graph.isRoot("3"));
        assertFalse(graph.isLeaf("3"));
        assertFalse(graph.isRoot("4"));
        assertFalse(graph.isLeaf("4"));
        assertFalse(graph.isRoot("5"));
        assertTrue(graph.isLeaf("5"));
        
        List<String> parents = graph.getSortedParents("5", false);
        assertEquals(4, parents.size());
        assertEquals("4", parents.get(0));
        assertEquals("2", parents.get(1));
        assertEquals("3", parents.get(2));
        assertEquals("1", parents.get(3));
    }
    
    public void testCyclicGraph() {
        DirectedGraph<String> graph = new DirectedGraph<String>();
        
        graph.addVertex("1");
        graph.addVertex("2");
        graph.addVertex("3");
        graph.addVertex("4");
        graph.addVertex("5");
        
        graph.addEdge("1", "2");
        graph.addEdge("3", "2");
        graph.addEdge("2", "4");
        graph.addEdge("3", "4");
        graph.addEdge("4", "5");
        graph.addEdge("5", "1");
        
        assertEquals(5, graph.size());
        assertFalse(graph.isRoot("1"));
        assertFalse(graph.isLeaf("1"));
        assertFalse(graph.isRoot("2"));
        assertFalse(graph.isLeaf("2"));
        assertTrue(graph.isRoot("3"));
        assertFalse(graph.isLeaf("3"));
        assertFalse(graph.isRoot("4"));
        assertFalse(graph.isLeaf("4"));
        assertFalse(graph.isRoot("5"));
        assertFalse(graph.isLeaf("5"));
        
        List<String> parents = graph.getSortedParents("5", false);
        assertEquals(4, parents.size());
        assertEquals("4", parents.get(0));
        assertEquals("2", parents.get(1));
        assertEquals("3", parents.get(2));
        assertEquals("1", parents.get(3));
    }
    
    @Test
    public void testSort() {
        final DirectedGraph<String> graph = new DirectedGraph<String>();
        
        graph.addVertex("1");
        graph.addVertex("2");
        graph.addVertex("3");
        graph.addVertex("4");
        graph.addVertex("5");
        graph.addVertex("6");
        graph.addVertex("7");
        graph.addVertex("8");
        graph.addVertex("9");
        
        graph.addEdge("1", "4");
        graph.addEdge("2", "4");
        graph.addEdge("2", "5");
        graph.addEdge("7", "5");
        graph.addEdge("2", "6");
        graph.addEdge("3", "6");
        graph.addEdge("8", "6");
        graph.addEdge("4", "7");
        graph.addEdge("5", "8");
        graph.addEdge("7", "9");
        graph.addEdge("8", "9");
        
        assertEquals(9, graph.size());
        assertTrue(graph.isRoot("1"));
        assertFalse(graph.isLeaf("1"));
        assertTrue(graph.isRoot("2"));
        assertFalse(graph.isLeaf("2"));
        assertTrue(graph.isRoot("3"));
        assertFalse(graph.isLeaf("3"));
        assertFalse(graph.isRoot("4"));
        assertFalse(graph.isLeaf("4"));
        assertFalse(graph.isRoot("5"));
        assertFalse(graph.isLeaf("5"));
        assertFalse(graph.isRoot("6"));
        assertTrue(graph.isLeaf("6"));
        assertFalse(graph.isRoot("7"));
        assertFalse(graph.isLeaf("7"));
        assertFalse(graph.isRoot("8"));
        assertFalse(graph.isLeaf("8"));
        assertFalse(graph.isRoot("9"));
        assertTrue(graph.isLeaf("9"));
        
        final List<String> sorted1 = graph.sortRootsToLeaves();
        assertEquals(9, sorted1.size());
        assertEquals("1", sorted1.get(0));
        assertEquals("2", sorted1.get(1));
        assertEquals("3", sorted1.get(2));
        assertEquals("4", sorted1.get(3));
        assertEquals("7", sorted1.get(4));
        assertEquals("5", sorted1.get(5));
        assertEquals("8", sorted1.get(6));
        assertEquals("6", sorted1.get(7));
        assertEquals("9", sorted1.get(8));
        
        final List<String> sorted2 = graph.sortLeavesToRoots();
        assertEquals(9, sorted2.size());
        assertEquals("6", sorted2.get(0));
        assertEquals("9", sorted2.get(1));
        assertEquals("8", sorted2.get(2));
        assertEquals("5", sorted2.get(3));
        assertEquals("7", sorted2.get(4));
        assertEquals("4", sorted2.get(5));
        assertEquals("1", sorted2.get(6));
        assertEquals("2", sorted2.get(7));
        assertEquals("3", sorted2.get(8));
    }
    
}
