package com.ixaris.commons.microservices.scslparser.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.ixaris.commons.microservices.scslparser.model.annotation.Required;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;

public class ScslParam extends ScslNode<ScslParam> {
    
    private static final String PARAMETER_TYPE = "parameter";
    
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("string",
        "double",
        "float",
        "int32",
        "uint32",
        "sint32",
        "fixed32",
        "sfixed32",
        "int64",
        "uint64",
        "sint64",
        "fixed64",
        "sfixed64"));
    
    @Required
    private String type;
    
    public ScslParam(final ScslModelObject<?> parent, final String name) {
        super(parent, name);
    }
    
    @Override
    public boolean parseEntry(final Entry<String, Object> entry) {
        
        final String name = entry.getKey();
        if (PARAMETER_TYPE.equals(name)) {
            type = validate(PARAMETER_TYPE, entry.getValue());
            if (!TYPES.contains(type)) {
                throw new ScslParseException("Unsupported parameter type " + type);
            }
        } else {
            return super.parseEntry(entry);
        }
        return true;
    }
    
    @Override
    public String getPathElement() {
        return "/{" + getName() + "}";
    }
    
    public String getType() {
        return type;
    }
    
}
