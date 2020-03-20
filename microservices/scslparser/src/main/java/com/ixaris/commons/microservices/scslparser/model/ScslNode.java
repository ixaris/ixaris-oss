package com.ixaris.commons.microservices.scslparser.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.ixaris.commons.microservices.scslparser.model.annotation.Required;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;

/**
 * Represents common information found within any non-leaf node inside the scsl document
 *
 * <p>Created by ian.grima on 22/03/2016.
 */
public abstract class ScslNode<T extends ScslNode<T>> extends ScslModelObject<T> {
    
    private static final String DESCRIPTION = "description";
    
    static final String TAGS = "tags";
    static final String SECURITY = "security";
    
    // First character can be any latin character, from second character onwards it can be any latin character, number
    // or _
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("^/" + IDENTIFIER_REGEX + "$");
    private static final Pattern PARAM_PATTERN = Pattern.compile("^/\\{" + IDENTIFIER_REGEX + "\\}$");
    
    @Required
    private String name;
    
    private String description;
    
    private String security;
    
    private List<String> tags;
    
    private Set<ScslMethod> methods = new LinkedHashSet<>();
    
    private ScslParam param;
    
    private Set<ScslResource> subResources = new LinkedHashSet<>();
    
    public ScslNode(final ScslModelObject<?> parent, final String name) {
        super(parent);
        this.name = name;
    }
    
    @Override
    public T parse(final Map<String, Object> yamlTree) {
        if (yamlTree != null) {
            yamlTree.entrySet().forEach((entry) -> {
                if (!parseEntry(entry)) {
                    throw new ScslParseException(entry);
                }
            });
        }
        
        return this.validate();
    }
    
    @SuppressWarnings("unchecked")
    public boolean parseEntry(final Entry<String, Object> entry) {
        final String key = entry.getKey();
        if (DESCRIPTION.equals(key)) {
            description = (String) entry.getValue();
        } else if (SECURITY.equals(key)) {
            security = (String) entry.getValue();
        } else if (TAGS.equals(key)) {
            tags = (List<String>) entry.getValue();
        } else if (IDENTIFIER_PATTERN.matcher(key).matches()) {
            addMethod(createChild(ScslMethod.class, key, entry.getValue()));
        } else if (PARAM_PATTERN.matcher(key).matches()) {
            setParam(createChild(ScslParam.class, key.substring(2, key.length() - 1), entry.getValue()));
        } else if (RESOURCE_PATTERN.matcher(key).matches()) {
            addSubResource(createChild(ScslResource.class, key.substring(1), entry.getValue()));
        } else {
            return false;
        }
        return true;
    }
    
    @Override
    public String getPathElement() {
        return "/" + name;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public String getSecurity() {
        return security;
    }
    
    public Set<ScslMethod> getMethods() {
        return Collections.unmodifiableSet(methods);
    }
    
    private void addMethod(final ScslMethod method) {
        methods.add(method);
    }
    
    public ScslParam getParam() {
        return param;
    }
    
    private void setParam(final ScslParam param) {
        if (this.param != null) {
            throw new ScslParseException("Already found param [" + this.param + "] while parsing param [" + param + "] in node [" + name + "]");
        }
        
        this.param = param;
    }
    
    public Set<ScslResource> getSubResources() {
        return Collections.unmodifiableSet(subResources);
    }
    
    private void addSubResource(final ScslResource scslResource) {
        subResources.add(scslResource);
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
    
}
