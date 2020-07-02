package com.ixaris.commons.jooq.persistence.test;

import org.junit.Assert;
import org.junit.Test;

import com.ixaris.commons.jooq.persistence.JsonConverter;

/**
 * Created by joseph.galea on 06/07/2017.
 */
public class JsonConverterTest {
    
    private final String testJsonString = "{\"age\": 31,"
        + "\"eyeColor\": \"green\","
        + "\"name\": {"
        + "\"first\": \"Mollie\","
        + "\"last\": \"Rollins\""
        + "},"
        + "\"company\": \"SIGNITY\"}";
    
    private final JsonConverter jsonConverter = new JsonConverter();
    
    @Test
    public void fromNullObject_noConversion_shouldReturnNull() {
        final String result = jsonConverter.from(null);
        Assert.assertNull(result);
    }
    
    @Test
    public void fromJsonStringObject_convertedToString_shouldReturnStringWithoutAnyTransformation() {
        final String result = jsonConverter.from(testJsonString);
        Assert.assertEquals(testJsonString, result);
    }
    
    @Test
    public void toNullObject_noConversion_shouldReturnNull() {
        final String result = jsonConverter.to(null);
        Assert.assertNull(result);
    }
    
    @Test
    public void toJsonStringObject_convertedToString_shouldReturnStringWithoutAnyTransformation() {
        final String result = jsonConverter.to(testJsonString);
        Assert.assertEquals(testJsonString, result);
    }
}
