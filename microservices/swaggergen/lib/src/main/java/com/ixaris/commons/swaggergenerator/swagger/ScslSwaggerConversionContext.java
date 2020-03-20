package com.ixaris.commons.swaggergenerator.swagger;

import java.util.LinkedHashMap;
import java.util.Map;

import io.swagger.models.Model;

/**
 * Context object that can be passed around the different stages of Swagger Generation to avoid duplicating definitions or other info
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
final class ScslSwaggerConversionContext {
    
    private final Map<String, Model> fullNameToModelMap = new LinkedHashMap<>();
    
    Map<String, Model> getFullNameToModelMap() {
        return fullNameToModelMap;
    }
}
