package com.ixaris.commons.microservices.scslparser.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.ixaris.commons.microservices.scslparser.model.annotation.Required;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslRequiredFieldNotFoundException;

/**
 * By convention any non root object type (anything besides {@link ScslDefinition}) has to have a constructor that accepts the parameters {@link
 * ScslModelObject} and {@link String}, i.e. that nodes parent and name.
 *
 * <p>Created by ian.grima on 15/03/2016.
 */
public abstract class ScslModelObject<T extends ScslModelObject<T>> {
    
    static final String IDENTIFIER_REGEX = "[a-z][a-z0-9_]*";
    static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^" + IDENTIFIER_REGEX + "$");
    
    private final ScslModelObject<?> parent;
    
    public ScslModelObject(final ScslModelObject<?> parent) {
        
        // Only the root level object should have a null parent
        if (!ScslDefinition.class.isAssignableFrom(this.getClass()) && parent == null) {
            throw new ScslParseException(this.getClass().getSimpleName() + " cannot have a null parent");
        }
        
        this.parent = parent;
    }
    
    public abstract T parse(final Map<String, Object> yamlTree);
    
    public abstract String getPathElement();
    
    public <S extends ScslModelObject<S>> S createChild(final Class<S> childType, final String name, final Object yamlSubTree) {
        try {
            
            if (childType.equals(ScslDefinition.class)) {
                throw new ScslParseException("ScslDefinition cannot be used as a child node");
            }
            
            final Constructor<S> childTypeConstructor = childType.getConstructor(ScslModelObject.class, String.class);
            final S child = childTypeConstructor.newInstance(this, name);
            return child.parse(validateSubTree(yamlSubTree));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new ScslParseException("Error Creating Child node of type: " + childType.getSimpleName() + " and name: " + name, e);
        }
    }
    
    /**
     * This method should be called after an {@link ScslModelObject} has been fully populated by default this method checks that all required
     * fields have been populated.
     *
     * @return {@link T}
     */
    @SuppressWarnings("unchecked")
    public T validate() {
        validateRequiredFields(this);
        return (T) this;
    }
    
    public ScslModelObject<?> getParent() {
        return parent;
    }
    
    public final String getPath() {
        final StringBuilder path = new StringBuilder();
        ScslModelObject<?> parent = getParent();
        while (parent != null) {
            final String element = parent.getPathElement();
            if (element != null && element.length() > 0) {
                path.insert(0, element);
            }
            parent = parent.getParent();
        }
        return path.toString();
    }
    
    /**
     * Retrieve all Path Parameters related to this node
     *
     * @return
     */
    public final Map<String, ScslParam> getPathParams() {
        final Map<String, ScslParam> params = new HashMap<>();
        ScslModelObject<?> p = getParent();
        while (p != null) {
            if (p instanceof ScslParam) {
                final ScslParam scslParam = (ScslParam) p;
                params.put(scslParam.getName(), scslParam);
            }
            p = p.getParent();
        }
        
        return params;
    }
    
    /**
     * For every string field we parse we check that it is not configured with a null or empty value, With regards to optional attributes if one
     * does not wish to set a particular optional attribute it should be omitted altogether not added to the scsl file with a null or empty value
     *
     * <p>This is Incorrect: /examples: description: null get: request: ExamplesFilter responses: 200: Examples 409: ExampleError
     *
     * <p>This is OK: /examples: get: request: ExamplesFilter responses: 200: Examples 409: ExampleError
     *
     * @param paramName
     * @param parsedObject
     * @return
     * @throws ScslParseException if a null or empty value is specified
     */
    String validate(final String paramName, final Object parsedObject) {
        final String value = String.valueOf(parsedObject);
        if (parsedObject == null || value.isEmpty()) {
            throw new ScslParseException((paramName == null ? "key" : paramName) + " should not be null or empty");
        }
        return value;
    }
    
    String validateIdentifier(final String paramName, final Object parsedObject) {
        final String value = String.valueOf(parsedObject);
        if (parsedObject == null || value.isEmpty() || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new ScslParseException((paramName == null ? "key" : paramName)
                + " should not be null or empty and should be a valid identifier, was "
                + value);
        }
        return value;
    }
    
    boolean validateBoolean(final String paramName, final Object parsedObject) {
        return Boolean.parseBoolean(validate(paramName, parsedObject));
    }
    
    @SuppressWarnings("unchecked")
    <K, V> Map<K, V> validateSubTree(final Object subtree) {
        if (subtree == null) {
            return null;
        } else if (subtree instanceof Map) {
            return (Map<K, V>) subtree;
        } else {
            throw new ScslParseException("Could not parse sub tree: " + subtree);
        }
    }
    
    /**
     * Fields are checked for given object for {@link Required} annotation if a field has this annotation and has a null value then a {@link
     * ScslRequiredFieldNotFoundException} is thrown
     *
     * @param object
     * @throws ScslParseException if an {@link IllegalAccessException} is caught while checking the given object's fields
     * @throws ScslRequiredFieldNotFoundException if a required field is null or empty
     */
    private void validateRequiredFields(final Object object) {
        final List<Field> fields = getAllFields(new ArrayList<>(), object.getClass()); // Stream all fields of given object
        fields.stream()
            .filter(field -> {
                final Required required = field.getAnnotation(Required.class);
                return (required != null) && required.required();
            }) // Filter out fields that are not annotated with Required or that have required set to false
            .forEach(field -> {
                try {
                    // Since all these fields are marked as required, we check that they are indeed not null or empty
                    // if a required field is found as null or empty we throw a ScslRequiredFieldNotFoundException
                    field.setAccessible(true);
                    final Object value = field.get(object);
                    if ((value == null)
                        || (value instanceof String && ((String) value).isEmpty())
                        || (value instanceof Collection && ((Collection<?>) value).isEmpty())
                        || (value instanceof Map && ((Map<?, ?>) value).isEmpty())) {
                        
                        throw new ScslRequiredFieldNotFoundException(field.getName());
                    }
                } catch (final IllegalAccessException e) {
                    throw new ScslParseException(e);
                }
            });
    }
    
    private static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));
        if (type.getSuperclass() != null) {
            fields = getAllFields(fields, type.getSuperclass());
        }
        return fields;
    }
}
