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
public class ScslResourceTest {
    
    private ScslResource resource = null;
    
    @Before
    public void setup() {
        resource = new ScslResource(new ScslDefinition(), "resource_name") {
            
            @Override
            public <S extends ScslModelObject<S>> S createChild(Class<S> childType, String name, Object yamlSubTree) {
                return ScslTestHelper.createDummyChild(childType, name);
            }
        };
    }
    
    @Test
    public void scsl_resource_object__success() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("post", new HashMap<>());
        testYamlTree.put("/{id}", new HashMap<>());
        testYamlTree.put("/subresource", new HashMap<>());
        final ScslResource scslResource = resource.parse(testYamlTree);
        
        assertEquals("resource_name", scslResource.getName());
        assertEquals("Test Description", scslResource.getDescription());
        assertEquals(1, scslResource.getMethods().size());
        assertNotNull(scslResource.getParam());
        assertEquals(1, scslResource.getSubResources().size());
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_resource_object_null_parent() {
        
        resource = new ScslResource(null, "resource_name") {
            
            @Override
            public <S extends ScslModelObject<S>> S createChild(Class<S> childType, String name, Object yamlSubTree) {
                return ScslTestHelper.createDummyChild(childType, name);
            }
        };
        
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("post", new HashMap<>());
        testYamlTree.put("/{id}", new HashMap<>());
        testYamlTree.put("/subresource", new HashMap<String, Object>() {
            
            {
                put("post", new HashMap<>());
            }
        });
        resource.parse(testYamlTree);
    }
    
    @Test
    public void scsl_resource_object__success_missing_non_required_attribute() {
        
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("post", new HashMap<>());
        final ScslResource scslResource = resource.parse(testYamlTree);
        
        assertEquals("resource_name", scslResource.getName());
        assertEquals("Test Description", scslResource.getDescription());
        assertEquals(1, scslResource.getMethods().size());
        assertNull(scslResource.getParam());
    }
    
    @Test(expected = ScslRequiredFieldNotFoundException.class)
    public void scsl_resource_object_missing_attribute() {
        resource = new ScslResource(new ScslDefinition(), null) {
            
            @Override
            public <S extends ScslModelObject<S>> S createChild(Class<S> childType, String name, Object yamlSubTree) {
                return ScslTestHelper.createDummyChild(childType, name);
            }
        };
        
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("post", new HashMap<>());
        testYamlTree.put("/{id}", new HashMap<>());
        resource.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_resource_object_unknown_attribute() {
        resource = new ScslResource(new ScslDefinition(), "resource_name");
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("unknownField", "unkownField");
        resource.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_resource_object_invalid_method_name() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("post", new HashMap<>());
        testYamlTree.put("1invalid", new HashMap<>());
        testYamlTree.put("/{id}", new HashMap<>());
        resource.parse(testYamlTree);
    }
    
}
