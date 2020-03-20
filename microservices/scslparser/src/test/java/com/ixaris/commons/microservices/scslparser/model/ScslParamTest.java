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
public class ScslParamTest {
    
    private ScslParam param = null;
    
    @Before
    public void setup() {
        param = new ScslParam(new ScslDefinition(), "param") {
            
            @Override
            public <S extends ScslModelObject<S>> S createChild(Class<S> childType, String name, Object yamlSubTree) {
                return ScslTestHelper.createDummyChild(childType, name);
            }
        };
    }
    
    @Test
    public void scsl_param_object__success() {
        
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("parameter", "int32");
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("post", new HashMap<>());
        testYamlTree.put("/subresource", new HashMap<>());
        final ScslParam scslParam = param.parse(testYamlTree);
        
        assertEquals("param", scslParam.getName());
        assertEquals("int32", scslParam.getType());
        assertEquals("Test Description", scslParam.getDescription());
        assertEquals(1, scslParam.getMethods().size());
        assertEquals(1, scslParam.getSubResources().size());
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_param_object_null_parent() {
        param = new ScslParam(null, "param") {
            
            @Override
            public <S extends ScslModelObject<S>> S createChild(Class<S> childType, String name, Object yamlSubTree) {
                return ScslTestHelper.createDummyChild(childType, name);
            }
        };
        
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("parameter", "int32");
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("post", new HashMap<>());
        testYamlTree.put("/subresource", new HashMap<>());
        param.parse(testYamlTree);
    }
    
    @Test(expected = ScslRequiredFieldNotFoundException.class)
    public void scsl_param_object_missing_attribute() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("post", new HashMap<>());
        param.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_param_object_null_value() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("parameter", null);
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("post", new HashMap<>());
        param.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_param_object_unknown_attribute() {
        param = new ScslParam(new ScslDefinition(), "param");
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("unknown", "unknown");
        param.parse(testYamlTree);
    }
    
    @Test(expected = ScslParseException.class)
    public void scsl_param_object_invalid_method_name() {
        final Map<String, Object> testYamlTree = new HashMap<>();
        testYamlTree.put("parameter", "int32");
        testYamlTree.put("description", "Test Description");
        testYamlTree.put("post", new HashMap<>());
        testYamlTree.put("1invalid", new HashMap<>());
        param.parse(testYamlTree);
    }
    
}
