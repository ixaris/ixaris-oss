package com.ixaris.commons.collections.lib;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.Test;

import com.ixaris.commons.collections.lib.WeightedDirectedGraph.Weighting;
import com.ixaris.commons.collections.lib.WeightedDirectedGraph.WeightingPreference;

public class WeightedDirectedGraphTest {
    
    @Test
    public void testSimpleGraph() {
        
        final Weighting<BigDecimal> weighting = new Weighting<BigDecimal>() {
            
            @Override
            public int compare(final BigDecimal o1, final BigDecimal o2) {
                return o1.compareTo(o2);
            }
            
            @Override
            public BigDecimal computeCombinedWeight(BigDecimal w1, BigDecimal w2) {
                return w1.multiply(w2);
            }
            
            @Override
            public BigDecimal getInitialWeight() {
                return BigDecimal.ONE;
            }
        };
        
        WeightedDirectedGraph<String, BigDecimal> graph = new WeightedDirectedGraph<String, BigDecimal>(weighting);
        
        graph.addVertex("GBP");
        graph.addVertex("USD");
        graph.addVertex("EUR");
        graph.addVertex("YEN");
        
        graph.addEdge("GBP", "USD", BigDecimal.valueOf(0.5));
        graph.addEdge("USD", "EUR", BigDecimal.valueOf(0.5));
        graph.addEdge("EUR", "YEN", BigDecimal.valueOf(0.5));
        
        assertEquals(4, graph.size());
        assertTrue(graph.isRoot("GBP"));
        assertFalse(graph.isLeaf("GBP"));
        assertFalse(graph.isRoot("USD"));
        assertFalse(graph.isLeaf("USD"));
        assertFalse(graph.isRoot("EUR"));
        assertFalse(graph.isLeaf("EUR"));
        assertFalse(graph.isRoot("YEN"));
        assertTrue(graph.isLeaf("YEN"));
        
        final Map<String, BigDecimal> children = graph.traverseChildren("GBP", WeightingPreference.ANY);
        assertEquals(3, children.size());
        assertTrue(BigDecimal.valueOf(0.25).compareTo(children.get("EUR")) == 0);
        assertTrue(BigDecimal.valueOf(0.125).compareTo(children.get("YEN")) == 0);
        assertTrue(BigDecimal.valueOf(0.5).compareTo(children.get("USD")) == 0);
        
        graph.addEdge("YEN", "GBP", BigDecimal.valueOf(8));
        
        final Map<String, BigDecimal> children2 = graph.traverseChildren("GBP", WeightingPreference.SMALLEST);
        assertEquals(4, children2.size());
        assertTrue(BigDecimal.valueOf(0.25).compareTo(children2.get("EUR")) == 0);
        assertTrue(BigDecimal.valueOf(0.125).compareTo(children2.get("YEN")) == 0);
        assertTrue(BigDecimal.valueOf(0.5).compareTo(children2.get("USD")) == 0);
        assertTrue(BigDecimal.ONE.compareTo(children2.get("GBP")) == 0);
    }
    
}
