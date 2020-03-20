package com.ixaris.commons.microservices.scslparser.model;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslRequiredFieldNotFoundException;
import com.ixaris.commons.microservices.scslparser.model.support.ScslTestHelper;

/**
 * Created by ian.grima on 16/03/2016.
 */
public class ScslDefinitionTest {
    
    private ScslDefinition definition = null;
    
    @Before
    public void setup() {
        definition = new ScslDefinition() {
            
            @Override
            public <S extends ScslModelObject<S>> S createChild(Class<S> childType, String name, Object yamlSubTree) {
                return ScslTestHelper.createDummyChild(childType, name);
            }
        };
    }
    
    @Test
    public void scsl_definition_object__success() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("title", "Test Title");
        testYamlTree.put("name", "root");
        testYamlTree.put("version", 4);
        testYamlTree.put("spi", true);
        testYamlTree.put("schema", "Test Schema");
        testYamlTree.put("basePackage", "Test Package");
        testYamlTree.put("context", "Test Context");
        testYamlTree.put("/{param}", new HashMap<>());
        testYamlTree.put("/bobby", new HashMap<>());
        definition.parse(testYamlTree);
        
        assertEquals("Test Title", definition.getTitle());
        assertEquals("root", definition.getName());
        assertEquals(4, definition.getVersion());
        assertTrue(definition.isSpi());
        assertEquals("Test Schema", definition.getSchema());
        assertEquals("Test Package", definition.getBasePackage());
        assertEquals("Test Context", definition.getContext());
        assertEquals(1, definition.getSubResources().size());
    }
    
    @Test(expected = ScslRequiredFieldNotFoundException.class)
    public void scsl_definition_object_missing_attribute() {
        
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("title", "Test Title");
        testYamlTree.put("spi", true);
        testYamlTree.put("schema", "Test Schema");
        testYamlTree.put("basePackage", "Test Package");
        testYamlTree.put("context", "Test Context");
        testYamlTree.put("/bobby", new HashMap<>());
        definition.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_definition_object_null_value() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("title", "Test Title");
        testYamlTree.put("name", "root");
        testYamlTree.put("spi", true);
        testYamlTree.put("version", "Test Version");
        testYamlTree.put("schema", null);
        testYamlTree.put("basePackage", "Test Package");
        testYamlTree.put("context", "Test Context");
        testYamlTree.put("/bobby", new HashMap<>());
        definition.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_definition_object_invalid_method_name() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("title", "Test Title");
        testYamlTree.put("name", "root");
        testYamlTree.put("spi", true);
        testYamlTree.put("version", "Test Version");
        testYamlTree.put("schema", "Test Schema");
        testYamlTree.put("basePackage", "Test Package");
        testYamlTree.put("context", "Test Context");
        testYamlTree.put("$unknown", "Unknown");
        testYamlTree.put("/bobby", new HashMap<>());
        definition.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_definition_object_as_child() {
        definition = new ScslDefinition() {
            
            @Override
            public boolean parseEntry(Map.Entry<String, Object> yamlTree) {
                createChild(ScslDefinition.class, "test", yamlTree);
                return true;
            }
        };
        
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("name", "test");
        definition.parse(testYamlTree);
    }
    
}
