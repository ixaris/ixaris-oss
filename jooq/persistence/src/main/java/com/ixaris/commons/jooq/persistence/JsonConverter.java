package com.ixaris.commons.jooq.persistence;

import org.jooq.impl.AbstractConverter;

public class JsonConverter extends AbstractConverter<Object, String> {
    
    public JsonConverter() {
        super(Object.class, String.class);
    }
    
    public String from(final Object jsonFieldValue) {
        return jsonFieldValue == null ? null : String.valueOf(jsonFieldValue);
    }
    
    @Override
    public String to(final String jsonString) {
        return jsonString;
    }
    
}
