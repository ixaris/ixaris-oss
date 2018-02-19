package com.ixaris.commons.async.lib;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

// TODO test with thread
public class AsyncLocalTest {
    
    private static final AsyncLocal<String> VAL1 = new AsyncLocal<>();
    private static final AsyncLocal<String> VAL2 = new AsyncLocal<>(true);
    
    @Test
    public void testSetValue() {
        assertThat(VAL1.get()).isNull();
        
        VAL1.exec("TEST", () -> assertThat(VAL1.get()).isEqualTo("TEST"));
        
        assertThat(VAL1.get()).isNull();
    }
    
    @Test
    public void testSetSameValue_expectException() {
        assertThat(VAL1.get()).isNull();
        VAL1.exec("TEST", () -> {
            assertThat(VAL1.get()).isEqualTo("TEST");
            
            VAL1.exec("TEST", () -> assertThat(VAL1.get()).isEqualTo("TEST"));
            
            assertThat(VAL1.get()).isEqualTo("TEST");
        });
        assertThat(VAL1.get()).isNull();
    }
    
    @Test
    public void testSetDifferentValue_expectException() {
        assertThat(VAL1.get()).isNull();
        VAL1.exec("TEST", () -> {
            assertThat(VAL1.get()).isEqualTo("TEST");
            
            assertThatThrownBy(() -> VAL1.exec("TEST2", () -> {})).isInstanceOf(IllegalStateException.class);
            
            assertThat(VAL1.get()).isEqualTo("TEST");
        });
        assertThat(VAL1.get()).isNull();
    }
    
    @Test
    public void testStackValue() {
        assertThat(VAL2.get()).isNull();
        VAL2.exec("1", () -> {
            assertThat(VAL2.get()).isEqualTo("1");
            VAL2.exec("2", () -> {
                assertThat(VAL2.get()).isEqualTo("2");
                VAL2.exec("3", () -> {
                    assertThat(VAL2.get()).isEqualTo("3");
                    
                    VAL2.exec("4", () -> assertThat(VAL2.get()).isEqualTo("4"));
                    
                    assertThat(VAL2.get()).isEqualTo("3");
                });
                assertThat(VAL2.get()).isEqualTo("2");
            });
            assertThat(VAL2.get()).isEqualTo("1");
        });
        assertThat(VAL2.get()).isNull();
    }
    
    @Test
    public void testSnapshot() {
        final Map<AsyncLocal<?>, Object> map = new HashMap<>();
        assertThat(AsyncLocal.snapshot().getMap()).isEqualTo(map);
        VAL1.exec("TEST", () -> {
            map.put(VAL1, "TEST");
            assertThat(AsyncLocal.snapshot().getMap()).isEqualTo(map);
            VAL2.exec("1", () -> {
                map.put(VAL2, "1");
                assertThat(AsyncLocal.snapshot().getMap()).isEqualTo(map);
                VAL2.exec("2", () -> {
                    map.put(VAL2, Arrays.asList("2", "1"));
                    assertThat(AsyncLocal.snapshot().getMap()).isEqualTo(map);
                });
                map.put(VAL2, "1");
                assertThat(AsyncLocal.snapshot().getMap()).isEqualTo(map);
            });
            map.remove(VAL2);
            assertThat(AsyncLocal.snapshot().getMap()).isEqualTo(map);
        });
        map.remove(VAL1);
        assertThat(AsyncLocal.snapshot().getMap()).isEqualTo(map);
    }
    
    @Test
    public void testBuilder() {
        assertThat(VAL1.get()).isNull();
        assertThat(VAL2.get()).isNull();
        
        AsyncLocal.with(VAL1, "TEST").with(VAL2, "1").exec(() -> {
            assertThat(VAL1.get()).isEqualTo("TEST");
            assertThat(VAL2.get()).isEqualTo("1");
            
            AsyncLocal.with(VAL2, "2").exec(() -> {
                assertThat(VAL1.get()).isEqualTo("TEST");
                assertThat(VAL2.get()).isEqualTo("2");
                
                assertThatThrownBy(() -> AsyncLocal.with(VAL1, "TEST2").with(VAL2, "3").exec(() -> {})).isInstanceOf(IllegalStateException.class);
                
                assertThat(VAL1.get()).isEqualTo("TEST");
                assertThat(VAL2.get()).isEqualTo("2");
            });
            assertThat(VAL1.get()).isEqualTo("TEST");
            assertThat(VAL2.get()).isEqualTo("1");
        });
        assertThat(VAL1.get()).isNull();
        assertThat(VAL2.get()).isNull();
    }
    
}
