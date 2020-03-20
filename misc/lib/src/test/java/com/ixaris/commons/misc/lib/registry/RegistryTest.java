package com.ixaris.commons.misc.lib.registry;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.ixaris.commons.misc.lib.registry.SomeTestEnum.SomeTestEnumContainer;

public class RegistryTest {
    
    @Test
    public void getRegisteredKeys_doesNotReturnArchivedValues() {
        TestTypeRegistry.getInstance().postConstruct();
        Registry.registerInApplicableRegistries(SomeTestType.getInstance());
        Registry.registerInApplicableRegistries(ArchivedTestType.getInstance());
        Arrays.stream(SomeTestEnumContainer.INSTANCE.getEnumValues()).forEach(Registry::registerInApplicableRegistries);
        
        assertEquals(3, TestTypeRegistry.getInstance().getRegisteredKeys().size());
        
        Set<String> expectedKeys = new HashSet<>();
        expectedKeys.add("TEST1");
        expectedKeys.add("TEST2");
        expectedKeys.add("TEST3");
        
        assertEquals(expectedKeys, TestTypeRegistry.getInstance().getRegisteredKeys());
        
        final TestType archive = TestTypeRegistry.getInstance().resolve("ARCHIVED");
        
        assertNotNull(archive);
    }
    
}
