package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.CompletionStageUtil.all;
import static com.ixaris.commons.async.lib.CompletionStageUtil.allOrderedFirstFailure;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ixaris.commons.misc.lib.object.Tuple3;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

public class CompletionStageUtilTest {
    
    @Test
    public void testAllSame() throws TimeoutException, InterruptedException {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        final CompletableFuture<Long> f3 = new CompletableFuture<>();
        
        final CompletionStage<Tuple3<Long, Long, Long>> fAll = all(f1, f2, f3);
        f1.complete(1L);
        f2.complete(2L);
        f3.complete(3L);
        
        final Tuple3<Long, Long, Long> result = block(fAll, 1, TimeUnit.MICROSECONDS);
        assertThat(result.get1().longValue()).isEqualTo(1L);
        assertThat(result.get2().longValue()).isEqualTo(2L);
        assertThat(result.get3().longValue()).isEqualTo(3L);
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
        
        final CompletionStage<Map<String, Long>> fAll = all(map);
        f1.complete(1L);
        f2.complete(2L);
        f3.complete(3L);
        
        final Map<String, Long> result = block(fAll, 1, TimeUnit.MICROSECONDS);
        assertThat(result).hasSize(3);
        assertThat(result.get("1").longValue()).isEqualTo(1L);
        assertThat(result.get("2").longValue()).isEqualTo(2L);
        assertThat(result.get("3").longValue()).isEqualTo(3L);
    }
    
    @Test
    public void testAll() throws TimeoutException, InterruptedException {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        final CompletableFuture<String> f3 = new CompletableFuture<>();
        
        final CompletionStage<List<Object>> fAll = all(Arrays.asList(f1, f2, f3));
        f1.complete(1L);
        f2.complete(2L);
        f3.complete("3");
        
        final List<Object> result = block(fAll, 1, TimeUnit.MICROSECONDS);
        assertThat(result).hasSize(3);
        assertThat(((Long) result.get(0)).longValue()).isEqualTo(1L);
        assertThat(((Long) result.get(1)).longValue()).isEqualTo(2L);
        assertThat(result.get(2)).isEqualTo("3");
    }
    
    @Test
    public void testAllMap() throws TimeoutException, InterruptedException {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        final CompletableFuture<String> f3 = new CompletableFuture<>();
        
        final Map<String, CompletionStage<?>> map = new HashMap<>();
        map.put("1", f1);
        map.put("2", f2);
        map.put("3", f3);
        
        final CompletionStage<Map<String, Object>> fAll = all(map);
        f1.complete(1L);
        f2.complete(2L);
        f3.complete("3");
        
        final Map<String, ?> result = block(fAll, 1, TimeUnit.MICROSECONDS);
        assertThat(result).hasSize(3);
        assertThat(((Long) result.get("1")).longValue()).isEqualTo(1L);
        assertThat(((Long) result.get("2")).longValue()).isEqualTo(2L);
        assertThat(result.get("3")).isEqualTo("3");
    }
    
    @Test
    public void testFailureShortcut() {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        final CompletableFuture<Long> f3 = new CompletableFuture<>();
        
        final CompletionStage<Tuple3<Long, Long, Long>> fAll = all(f1, f2, f3);
        
        f1.completeExceptionally(new RuntimeException("1"));
        
        assertThat(CompletionStageUtil.isDone(fAll)).isTrue();
        assertThat(CompletionStageUtil.isRejected(fAll)).isTrue();
        
        assertThatThrownBy(() -> block(fAll)).isInstanceOf(RuntimeException.class).hasMessage("1");
    }
    
    @Test
    public void testAllOrderedFirstFailure_success() {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        
        final CompletionStage<List<Object>> fAll = allOrderedFirstFailure(Arrays.asList(f1, f2));
        
        f1.complete(1L);
        f2.complete(2L);
        
        assertThat(CompletionStageUtil.isDone(fAll)).isTrue();
        assertThat(CompletionStageUtil.isFulfilled(fAll)).isTrue();
    }
    
    @Test
    public void testAllOrderedFirstFailure_firstFails() {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        
        final CompletionStage<List<Object>> fAll = allOrderedFirstFailure(Arrays.asList(f1, f2));
        
        f1.completeExceptionally(new RuntimeException("1"));
        
        assertThat(CompletionStageUtil.isDone(fAll)).isTrue();
        assertThat(CompletionStageUtil.isRejected(fAll)).isTrue();
        
        assertThatThrownBy(() -> block(fAll)).isInstanceOf(RuntimeException.class).hasMessage("1");
    }
    
    @Test
    public void testAllOrderedFirstFailure() {
        final CompletableFuture<Long> f1 = new CompletableFuture<>();
        final CompletableFuture<Long> f2 = new CompletableFuture<>();
        final CompletableFuture<String> f3 = new CompletableFuture<>();
        final CompletableFuture<String> f4 = new CompletableFuture<>();
        
        final CompletionStage<List<Object>> fAll = allOrderedFirstFailure(Arrays.asList(f1, f2, f3, f4));
        
        assertThat(CompletionStageUtil.isDone(fAll)).isFalse();
        
        f4.completeExceptionally(new RuntimeException("4"));
        
        assertThat(CompletionStageUtil.isDone(fAll)).isFalse();
        
        f1.complete(1L);
        
        assertThat(CompletionStageUtil.isDone(fAll)).isFalse();
        
        f2.completeExceptionally(new RuntimeException("2"));
        
        assertThat(CompletionStageUtil.isDone(fAll)).isTrue();
        assertThat(CompletionStageUtil.isRejected(fAll)).isTrue();
        
        assertThatThrownBy(() -> block(fAll)).isInstanceOf(RuntimeException.class).hasMessage("2");
    }
    
}
