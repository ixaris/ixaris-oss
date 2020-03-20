package com.ixaris.commons.microservices.lib.service.proxy;

import java.util.function.Function;

public enum PathParamType {
    
    INTEGER(Integer::valueOf),
    LONG(Long::valueOf),
    STRING(v -> v);
    
    public static PathParamType match(final Class<?> c) {
        if (c == int.class) {
            return INTEGER;
        } else if (c == long.class) {
            return LONG;
        } else if (c == String.class) {
            return STRING;
        } else {
            return null;
        }
    }
    
    private final Function<String, Object> mapFunction;
    
    PathParamType(final Function<String, Object> mapFunction) {
        this.mapFunction = mapFunction;
    }
    
    public final Object fromString(final String value) {
        return mapFunction.apply(value);
    }
    
}
