package com.ixaris.commons.misc.lib.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ixaris.commons.misc.lib.registry.SomeTestEnum.SomeTestEnumContainer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class RegistryTest {
    
    @Test
    public void getRegisteredKeys_doesNotReturnArchivedValues() {
        TestTypeRegistry.getInstance().postConstruct();
        Registry.registerToApplicableRegistries(SomeTestType.getInstance());
        Registry.registerToApplicableRegistries(ArchivedTestType.getInstance());
        Arrays.stream(SomeTestEnumContainer.INSTANCE.getEnumValues()).forEach(Registry::registerToApplicableRegistries);
        
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
