package com.ixaris.commons.microservices.scslparser;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;

import org.junit.Test;

import com.ixaris.commons.microservices.scslparser.model.ScslDefinition;
import com.ixaris.commons.microservices.scslparser.model.ScslMethod;
import com.ixaris.commons.microservices.scslparser.model.ScslParam;
import com.ixaris.commons.microservices.scslparser.model.ScslResource;
import com.ixaris.commons.microservices.scslparser.model.ScslResponses;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslRequiredFieldNotFoundException;

/**
 * Created by ian.grima on 07/03/2016.
 */
public class ScslParserTest {
    
    @Test
    public void test() {
        final ScslDefinition scslDefinition = ScslParser.parse("example.scsl", getClass().getClassLoader()::getResourceAsStream);
    }
    
    @Test
    public void parse_test_contract_success_from_file() {
        final ScslDefinition scslDefinition = ScslParser.parse("test_contract.scsl", getClass().getClassLoader()::getResourceAsStream);
        
        // Assert that parsed object contents match contract specification.
        assertNotNull(scslDefinition);
        assertEquals("Example Service Contract", scslDefinition.getTitle());
        assertEquals("../proto/example.proto", scslDefinition.getSchema());
        assertEquals("test.ixaris.commons.microservices.template.example", scslDefinition.getBasePackage());
        assertEquals("ExampleContext", scslDefinition.getContext());
        assertEquals(3, scslDefinition.getTags().size());
        assertEquals("A", scslDefinition.getTags().get(0));
        assertEquals("B", scslDefinition.getTags().get(1));
        assertEquals("C", scslDefinition.getTags().get(2));
        assertEquals(2, scslDefinition.getConstants().size());
        assertEquals("test 1", scslDefinition.getConstants().get("test1"));
        assertEquals("test 2", scslDefinition.getConstants().get("test2"));
        
        assertEquals(1, scslDefinition.getSubResources().size());
        
        final ScslParam rootParam = scslDefinition.getParam();
        
        assertNull(rootParam.getDescription());
        assertEquals("int32", rootParam.getType());
        assertEquals(1, rootParam.getMethods().size());
        
        final ScslMethod rootParamGetMethod = rootParam.getMethods().stream().findFirst().get();
        
        assertNull(rootParamGetMethod.getDescription());
        assertNull(rootParamGetMethod.getRequest());
        assertTrue(rootParamGetMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.SUCCESS, "Example")));
        assertTrue(rootParamGetMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.CONFLICT, "ExampleError")));
        
        final ScslResource resource = scslDefinition.getSubResources().stream().findFirst().get();
        
        assertEquals("examples", resource.getName());
        assertEquals("This is a nice resource", resource.getDescription());
        assertEquals(3, resource.getMethods().size());
        assertNotNull(resource.getParam());
        
        final ScslMethod getMethod = resource.getMethods().stream().filter((m) -> "get".equals(m.getName())).findFirst().get();
        
        assertNull(getMethod.getDescription());
        assertEquals("ExamplesFilter", getMethod.getRequest());
        assertTrue(getMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.SUCCESS, "Examples")));
        assertTrue(getMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.CONFLICT, "ExampleError")));
        
        final ScslMethod postMethod = resource.getMethods().stream().filter((m) -> "post".equals(m.getName())).findFirst().get();
        
        assertNull(postMethod.getDescription());
        assertEquals("Example", postMethod.getRequest());
        assertTrue(postMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.SUCCESS, "Example")));
        assertTrue(postMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.CONFLICT, "ExampleError")));
        
        final ScslMethod watchMethod = resource.getMethods().stream().filter((m) -> "watch".equals(m.getName())).findFirst().get();
        
        assertNull(watchMethod.getDescription());
        assertNull(watchMethod.getRequest());
        assertTrue(watchMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.SUCCESS, "ExampleEvent")));
        
        final ScslParam param = resource.getParam();
        
        assertNull(param.getDescription());
        assertEquals("int32", param.getType());
        assertEquals(3, param.getMethods().size());
        
        final ScslMethod paramGetMethod = param.getMethods().stream().filter((m) -> "get".equals(m.getName())).findFirst().get();
        
        assertNull(paramGetMethod.getDescription());
        assertNull(paramGetMethod.getRequest());
        assertTrue(paramGetMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.SUCCESS, "Example")));
        assertTrue(paramGetMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.CONFLICT, "ExampleError")));
        
        final ScslMethod paramPatchMethod = param.getMethods().stream().filter((m) -> "patch".equals(m.getName())).findFirst().get();
        
        assertNull(paramPatchMethod.getDescription());
        assertEquals("Example", paramPatchMethod.getRequest());
        assertTrue(paramPatchMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.SUCCESS, "Example")));
        assertTrue(paramPatchMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.CONFLICT, "ExampleError")));
        
        final ScslMethod paramDeleteMethod = param.getMethods().stream().filter((m) -> "delete".equals(m.getName())).findFirst().get();
        
        assertNull(paramDeleteMethod.getDescription());
        assertNull(paramDeleteMethod.getRequest());
        assertTrue(paramDeleteMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.SUCCESS, "Example")));
        assertTrue(paramDeleteMethod.getResponses().entrySet().contains(new AbstractMap.SimpleEntry<>(ScslResponses.CONFLICT, "ExampleError")));
    }
    
    @Test
    public void parse_test_contract_equality_success() {
        final ScslDefinition firstParse = ScslParser.parse("test_contract.scsl", getClass().getClassLoader()::getResourceAsStream);
        final ScslDefinition secondParse = ScslParser.parse("test_contract.scsl", getClass().getClassLoader()::getResourceAsStream);
        assertTrue(firstParse.equals(secondParse));
    }
    
    @Test
    public void parse_test_contract_not_equal() {
        final ScslDefinition firstParse = ScslParser.parse("test_contract.scsl", getClass().getClassLoader()::getResourceAsStream);
        final ScslDefinition secondParse = ScslParser.parse("test_contract2.scsl", getClass().getClassLoader()::getResourceAsStream);
        assertFalse(firstParse.equals(secondParse));
    }
    
    @Test
    public void parse_test_contract_hashcode_not_equal() {
        final ScslDefinition firstParse = ScslParser.parse("test_contract.scsl", getClass().getClassLoader()::getResourceAsStream);
        final ScslDefinition secondParse = ScslParser.parse("test_contract.scsl", getClass().getClassLoader()::getResourceAsStream);
        assertEquals(firstParse.hashCode(), secondParse.hashCode());
    }
    
    @Test(expected = ScslParseException.class)
    public void parse_test_malformed_contract_from_file() {
        ScslParser.parse("test_malformed_contract.scsl", getClass().getClassLoader()::getResourceAsStream);
    }
    
    @Test(expected = ScslRequiredFieldNotFoundException.class)
    public void parse_test_missing_field_contract() {
        ScslParser.parse("test_missing_field_contract.scsl", getClass().getClassLoader()::getResourceAsStream);
    }
    
    @Test(expected = ScslParseException.class)
    public void parse_test_contract_unexpected_exception_wrapped_successfully() {
        
        final InputStream dummy = new InputStream() {
            
            @Override
            public int read() throws IOException {
                throw new RuntimeException("test exception");
            }
            
        };
        
        ScslParser.parse("_", l -> dummy);
    }
}
