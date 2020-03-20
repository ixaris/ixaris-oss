package com.ixaris.commons.microservices.scslparser.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.ixaris.commons.microservices.scslparser.model.annotation.Required;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

/**
 * Represents the leaf nodes of the scsl document
 *
 * <p>Created by ian.grima on 07/03/2016.
 */
public class ScslMethod extends ScslModelObject<ScslMethod> {
    
    private static final String REQUEST_OBJECT = "request";
    private static final String RESPONSES = "responses";
    private static final String DESCRIPTION = "description";
    
    // Watch is reserved method, we do not allow paths with parameters to have watch,
    // watch is only allowed for fixed paths.
    private static final String WATCH = "watch";
    
    @Required
    private String name;
    
    private String description;
    
    private String security;
    
    private List<String> tags;
    
    private String request;
    
    private Map<ScslResponses, String> responses = new EnumMap<>(ScslResponses.class);
    
    public ScslMethod(final ScslModelObject<?> parent, final String name) {
        super(parent);
        this.name = name;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ScslMethod parse(final Map<String, Object> yamlTree) {
        if (yamlTree != null) {
            yamlTree.entrySet().forEach((entry) -> {
                final String key = entry.getKey();
                if (DESCRIPTION.equals(key)) {
                    description = (String) entry.getValue();
                } else if (ScslNode.SECURITY.equals(key)) {
                    security = (String) entry.getValue();
                } else if (ScslNode.TAGS.equals(key)) {
                    tags = (List<String>) entry.getValue();
                } else if (REQUEST_OBJECT.equals(key)) {
                    request = validate(REQUEST_OBJECT, entry.getValue());
                } else if (RESPONSES.equals(key)) {
                    parseResponses(validateSubTree(entry.getValue()));
                } else {
                    throw new ScslParseException(entry);
                }
            });
        }
        
        return this.validate();
    }
    
    @Override
    public String getPathElement() {
        return null;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getSecurity() {
        return security;
    }
    
    public List<String> getTags() {
        return Optional.ofNullable(tags).orElse(Collections.emptyList());
    }
    
    public String getRequest() {
        return request;
    }
    
    public Map<ScslResponses, String> getResponses() {
        return Collections.unmodifiableMap(responses);
    }
    
    @Override
    public ScslMethod validate() {
        // First perform general validations
        final ScslMethod validatedModel = super.validate();
        
        // Perform ScslMethod specific validations
        if (WATCH.equalsIgnoreCase(getName())) {
            ScslModelObject<?> parent = getParent();
            while (parent != null) {
                if (parent instanceof ScslParam) {
                    throw new ScslParseException("Invalid watch on a parametrised path");
                }
                parent = parent.getParent();
            }
            if (getResponses().get(ScslResponses.SUCCESS) == null) {
                throw new ScslParseException("Invalid watch without success response");
            }
            if (getResponses().get(ScslResponses.CONFLICT) != null) {
                throw new ScslParseException("Invalid watch with conflict response");
            }
            if (getSecurity() != null) {
                throw new ScslParseException("Invalid watch with security");
            }
        }
        return validatedModel;
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("name", name).toString();
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, that -> Objects.equals(name, that.name));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    private void parseResponses(final Map<String, String> responses) {
        responses.forEach((key, value) -> this.responses.put(ScslResponses.parse(key), validate("response type", value)));
    }
    
}
