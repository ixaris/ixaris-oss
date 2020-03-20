package com.ixaris.commons.microservices.scslparser.model;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.ixaris.commons.microservices.scslparser.model.annotation.Required;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

public class ScslDefinition extends ScslNode<ScslDefinition> {
    
    private static final String TITLE = "title";
    private static final String NAME = "name";
    private static final String VERSION = "version";
    private static final String SPI = "spi";
    private static final String SCHEMA = "schema";
    private static final String BASE_PACKAGE = "basePackage";
    private static final String CONTEXT = "context";
    private static final String CONSTANTS = "constants";
    
    private String title;
    
    @Required
    private String name;
    
    private int version;
    
    private boolean spi;
    
    @Required
    private String schema;
    
    @Required
    private String basePackage;
    
    @Required
    private String context;
    
    private Map<String, Object> constants;
    
    /**
     * The ScslDefinition is the root level node thus it should not have a parent
     */
    public ScslDefinition() {
        super(null, "root");
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean parseEntry(Entry<String, Object> entry) {
        // Populate ScslDefinition from yamlTree
        final String key = entry.getKey();
        if (TITLE.equals(key)) {
            title = validate(TITLE, entry.getValue());
        } else if (NAME.equals(key)) {
            name = validateIdentifier(NAME, entry.getValue());
        } else if (VERSION.equals(key)) {
            version = Integer.parseInt(validate(VERSION, entry.getValue()));
        } else if (SPI.equals(key)) {
            spi = validateBoolean(SPI, entry.getValue());
        } else if (SCHEMA.equals(key)) {
            schema = validate(SCHEMA, entry.getValue());
        } else if (BASE_PACKAGE.equals(key)) {
            basePackage = validate(BASE_PACKAGE, entry.getValue());
        } else if (CONTEXT.equals(key)) {
            context = validate(CONTEXT, entry.getValue());
        } else if (CONSTANTS.equals(key)) {
            constants = validateSubTree(entry.getValue());
            constants
                .keySet()
                .stream()
                .filter(k -> !IDENTIFIER_PATTERN.matcher(k).matches())
                .findAny()
                .ifPresent(k -> {
                    throw new ScslParseException("Invalid constant " + k);
                });
        } else {
            return super.parseEntry(entry);
        }
        
        return true;
    }
    
    @Override
    public String getPathElement() {
        return "";
    }
    
    public String getTitle() {
        return title != null ? title : name; // default to name if title not available
    }
    
    public String getName() {
        return name;
    }
    
    public int getVersion() {
        return version;
    }
    
    public boolean isSpi() {
        return spi;
    }
    
    public String getSchema() {
        return schema;
    }
    
    public String getBasePackage() {
        return basePackage;
    }
    
    public String getContext() {
        return context;
    }
    
    public Map<String, Object> getConstants() {
        return constants;
    }
    
    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            final ScslDefinition that = (ScslDefinition) o;
            return Objects.equals(name, that.name)
                && (version == that.version)
                && Objects.equals(basePackage, that.basePackage)
                && Objects.equals(context, that.context);
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, version, basePackage, context);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("name", name).with("basePackage", basePackage).toString();
    }
    
}
