package com.ixaris.commons.misc.lib.logging;

import static org.mockito.ArgumentMatchers.any;

import com.ixaris.commons.misc.lib.logging.Logger.Level;
import com.ixaris.commons.misc.lib.logging.spi.LoggerFactorySpi;
import com.ixaris.commons.misc.lib.logging.spi.LoggerSpi;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class LoggerFactoryTest {
    
    private static LoggerFactorySpi SPI;
    private static final LoggerFactorySpi MOCK_FACTORY_SPI = Mockito.mock(LoggerFactorySpi.class);
    
    @BeforeAll
    public static void beforeClass() throws ReflectiveOperationException {
        final Field field = LoggerFactory.class.getDeclaredField("LOGGER_FACTORY_IMPL");
        field.setAccessible(true);
        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        SPI = (LoggerFactorySpi) field.get(null);
        field.set(null, MOCK_FACTORY_SPI);
    }
    
    @AfterAll
    public static void afterClass() throws ReflectiveOperationException {
        Field field = LoggerFactory.class.getDeclaredField("LOGGER_FACTORY_IMPL");
        field.setAccessible(true);
        field.set(null, SPI);
    }
    
    @Test
    public void test() {
        final LoggerSpi mockSpi = Mockito.mock(LoggerSpi.class);
        Mockito.when(mockSpi.isEnabled(any())).thenReturn(true);
        
        final ArgumentCaptor<Class> argumentCaptor = ArgumentCaptor.forClass(Class.class);
        Mockito.when(MOCK_FACTORY_SPI.getLogger(argumentCaptor.capture())).thenReturn(mockSpi);
        
        LogTest.LOG.atInfo().log("test");
        
        Assertions.assertThat(argumentCaptor.getValue()).isEqualTo(LogTest.class);
        Mockito.verify(mockSpi).isEnabled(Level.INFO);
        Mockito.verify(mockSpi).log(Level.INFO, null, "test");
    }
    
    private static class LogTest {
        
        private static final Logger LOG = LoggerFactory.forEnclosingClass();
        
    }
    
}
