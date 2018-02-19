package com.ixaris.commons.misc.lib.function;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class ConsumerAndFunctionTest {
    
    @Test(expected = IOException.class)
    public void exceptionalConsumerAsConsumer_ThrowsException_ShouldThrowRuntimeException() {
        ConsumerThrows.asConsumer(o -> {
            throw new IOException();
        }).accept("test");
    }
    
    @Test
    public void exceptionalConsumerAsConsumer_DoesNotThrowException_PerformsConsumerAction() {
        final AtomicBoolean b = new AtomicBoolean(false);
        ConsumerThrows.asConsumer(t -> b.set(true)).accept("test");
        assertThat(b.get()).isTrue();
    }
    
    @Test(expected = IOException.class)
    public void exceptionalFunctionAsFunction_ThrowsException_ShouldThrowRuntimeException() {
        FunctionThrows.asFunction(t -> {
            throw new IOException();
        }).apply("test");
    }
    
    @Test
    public void exceptionalFunctionAsFunction_DoesNotThrowException_ReturnsFunctionResult() {
        assertThat(FunctionThrows.asFunction(t -> true).apply("test")).isTrue();
    }
    
}
