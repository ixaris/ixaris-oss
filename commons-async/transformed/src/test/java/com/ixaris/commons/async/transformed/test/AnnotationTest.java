package com.ixaris.commons.async.transformed.test;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;

public class AnnotationTest {
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Marker {
        
    }
    
    public static class Annotated {
        
        @Marker
        public Async<Void> annotated() {
            return result();
        }
        
        @Marker
        public Async<Void> annotatedWithAwait() {
            await(annotated());
            return result();
        }
        
    }
    
    @Test
    public void testAnnotationMovedToGeneratedAsyncMethod() throws NoSuchMethodException {
        assertNull(Annotated.class.getMethod("annotated").getAnnotation(Marker.class));
        assertNotNull(Annotated.class.getMethod("async$annotated").getAnnotation(Marker.class));
        
        assertNull(Annotated.class.getMethod("annotatedWithAwait").getAnnotation(Marker.class));
        assertNotNull(Annotated.class.getMethod("async$annotatedWithAwait").getAnnotation(Marker.class));
        
        // look in declared methods since method is synthetic and also it will have parameters in the signature
        Arrays
            .stream(Annotated.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("continuation$async$annotatedWithAwait"))
            .findAny()
            .map(m -> {
                assertNull(m.getAnnotation(Marker.class));
                return m;
            })
            .orElseThrow(() -> new IllegalStateException("Should have method continuation$async$annotatedWithAwait"));
    }
    
}
