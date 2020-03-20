package com.ixaris.commons.microservices.scslparser.model;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;
import com.ixaris.commons.microservices.scslparser.model.support.ScslTestHelper;

/**
 * Created by ian.grima on 16/03/2016.
 */
public class ScslMethodTest {
    
    private ScslMethod method = null;
    
    @Before
    public void setup() {
        method = new ScslMethod(new ScslDefinition(), "get") {
            
            @Override
            public <S extends ScslModelObject<S>> S createChild(Class<S> childType, String name, Object yamlSubTree) {
                return ScslTestHelper.createDummyChild(childType, name);
            }
        };
    }
    
    @Test
    public void scsl_method_object__success() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("security", "TEST");
        testYamlTree.put("request", "Test Request");
        final Map<String, String> testResponses = new HashMap<>();
        testResponses.put("success", "Success Response");
        testResponses.put("conflict", "Failure Response");
        testYamlTree.put("responses", testResponses);
        
        final ScslMethod scslMethod = method.parse(testYamlTree);
        assertEquals("Test Description", scslMethod.getDescription());
        assertEquals("TEST", scslMethod.getSecurity());
        assertEquals("get", scslMethod.getName());
        assertEquals("Test Request", scslMethod.getRequest());
        assertEquals(2, scslMethod.getResponses().size());
        assertTrue(scslMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.SUCCESS, "Success Response")));
        assertTrue(scslMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.CONFLICT, "Failure Response")));
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_method_null_parent() {
        method = new ScslMethod(null, "get") {
            
            @Override
            public <S extends ScslModelObject<S>> S createChild(Class<S> childType, String name, Object yamlSubTree) {
                return ScslTestHelper.createDummyChild(childType, name);
            }
        };
        
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("request", "Test Request");
        final Map<String, String> testResponses = new HashMap<>();
        testResponses.put("success", "Success Response");
        testResponses.put("conflict", "Failure Response");
        testYamlTree.put("responses", testResponses);
        
        method.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_method_object_watch_with_param_parent_constraint_failure() {
        method = new ScslMethod(new ScslParam(new ScslDefinition(), "param"), "watch") {
            
            @Override
            public <S extends ScslModelObject<S>> S createChild(Class<S> childType, String name, Object yamlSubTree) {
                return ScslTestHelper.createDummyChild(childType, name);
            }
        };
        
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("request", "Test Request");
        final Map<String, String> testResponses = new HashMap<>();
        testResponses.put("success", "Success Response");
        testResponses.put("conflict", "Failure Response");
        testYamlTree.put("responses", testResponses);
        
        method.parse(testYamlTree);
    }
    
    @Test
    public void scsl_method_object_missing_response() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("request", "Test Request");
        
        final ScslMethod scslMethod = method.parse(testYamlTree);
        assertEquals("Test Description", scslMethod.getDescription());
        assertEquals("get", scslMethod.getName());
        assertEquals("Test Request", scslMethod.getRequest());
        assertEquals(0, scslMethod.getResponses().size());
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_method_object_null_value() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("request", "Test Request");
        final Map<String, String> testResponses = new HashMap<>();
        testResponses.put("success", null);
        testResponses.put("conflict", "Failure Response");
        testYamlTree.put("responses", testResponses);
        
        method.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_method_object_invalid_response_code() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("request", "Test Request");
        final Map<String, String> testResponses = new HashMap<>();
        testResponses.put("success", "Success Response");
        testResponses.put("invalid", "Failure Response");
        testYamlTree.put("responses", testResponses);
        
        method.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_method_object_unknown_attribute() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("request", "Test Request");
        testYamlTree.put("unknown", "Unknown");
        final Map<String, String> testResponses = new HashMap<>();
        testResponses.put("success", "Success Response");
        testResponses.put("conflict", "Failure Response");
        testYamlTree.put("responses", testResponses);
        
        method.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_method_object_invalid_method_name() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("request", "Test Request");
        testYamlTree.put("1invalid", new HashMap<>());
        final Map<String, String> testResponses = new HashMap<>();
        testResponses.put("success", "Success Response");
        testResponses.put("conflict", "Failure Response");
        testYamlTree.put("responses", testResponses);
        
        method.parse(testYamlTree);
    }
    
}
