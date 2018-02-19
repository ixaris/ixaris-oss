package com.ixaris.commons.async.lib;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class CompletionStageUtilTest {
    
    @Test
    public void testAllSame() throws TimeoutException, InterruptedException {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        final CompletableFuture<Long> f3 = new CompletableFuture<>();
        
        final CompletionStage<List<Long>> fAll = CompletionStageUtil.allSame(f1, f2, f3);
        f1.complete(1L);
        f2.complete(2L);
        f3.complete(3L);
        
        final List<Long> result = CompletionStageUtil.block(fAll, 1, TimeUnit.MICROSECONDS);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).longValue());
        assertEquals(2L, result.get(1).longValue());
        assertEquals(3L, result.get(2).longValue());
    }
    
    @Test
    public void testAllSameMap() throws TimeoutException, InterruptedException {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        final CompletableFuture<Long> f3 = new CompletableFuture<>();
        
        final Map<String, CompletionStage<Long>> map = new HashMap<>();
        map.put("1", f1);
        map.put("2", f2);
        map.put("3", f3);
        
        final CompletionStage<Map<String, Long>> fAll = CompletionStageUtil.allSame(map);
        f1.complete(1L);
        f2.complete(2L);
        f3.complete(3L);
        
        final Map<String, Long> result = CompletionStageUtil.block(fAll, 1, TimeUnit.MICROSECONDS);
        assertEquals(3, result.size());
        assertEquals(1L, result.get("1").longValue());
        assertEquals(2L, result.get("2").longValue());
        assertEquals(3L, result.get("3").longValue());
    }
    
    @Test
    public void testAll() throws TimeoutException, InterruptedException {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        final CompletableFuture<Long> f3 = new CompletableFuture<>();
        
        final CompletionStage<List<Object>> fAll = CompletionStageUtil.all(Arrays.asList(f1, f2, f3));
        f1.complete(1L);
        f2.complete(2L);
        f3.complete(3L);
        
        final List<Object> result = CompletionStageUtil.block(fAll, 1, TimeUnit.MICROSECONDS);
        assertEquals(3, result.size());
        assertEquals(1L, ((Long) result.get(0)).longValue());
        assertEquals(2L, ((Long) result.get(1)).longValue());
        assertEquals(3L, ((Long) result.get(2)).longValue());
    }
    
    @Test
    public void testAllMap() throws TimeoutException, InterruptedException {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        final CompletableFuture<Long> f3 = new CompletableFuture<>();
        
        final Map<String, CompletionStage<?>> map = new HashMap<>();
        map.put("1", f1);
        map.put("2", f2);
        map.put("3", f3);
        
        final CompletionStage<Map<String, ?>> fAll = CompletionStageUtil.all(map);
        f1.complete(1L);
        f2.complete(2L);
        f3.complete(3L);
        
        final Map<String, ?> result = CompletionStageUtil.block(fAll, 1, TimeUnit.MICROSECONDS);
        assertEquals(3, result.size());
        assertEquals(1L, ((Long) result.get("1")).longValue());
        assertEquals(2L, ((Long) result.get("2")).longValue());
        assertEquals(3L, ((Long) result.get("3")).longValue());
    }
    
}
