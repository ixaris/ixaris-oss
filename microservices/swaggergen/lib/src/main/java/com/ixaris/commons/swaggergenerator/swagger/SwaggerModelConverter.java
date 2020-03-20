package com.ixaris.commons.swaggergenerator.swagger;

import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;

import description.Description;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.AbstractNumericProperty;
import io.swagger.models.properties.AbstractProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import valid.Valid;

/**
 * Utilities to convert from Protobuf Classes to Swagger Models
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
final class SwaggerModelConverter {
    
    private static final Logger LOG = LoggerFactory.getLogger(SwaggerModelConverter.class);
    private static final Map<String, Model> CREATING = new HashMap<>();
    
    private SwaggerModelConverter() {}
    
    static Model getOrCreateModelFromClass(final ScslSwaggerConversionContext context, final Class<?> protobufClazz) {
        try {
            final Method getDescriptorMethod = protobufClazz.getMethod("getDescriptor");
            final Descriptor descriptor = (Descriptor) getDescriptorMethod.invoke(null);
            
            return getOrCreateModelFromDescriptor(context, descriptor);
        } catch (final NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Invalid request type. Unable to extract swagger model.", e);
        }
    }
    
    static Model getOrCreateModelFromDescriptor(final ScslSwaggerConversionContext context, final Descriptor descriptor) {
        Model model = context.getFullNameToModelMap().get(descriptor.getFullName());
        // do NOT convert the below to computeIfAbsent because LinkedHashMap does not behave correctly with nested
        // computeIfAbsents
        // which can happen due to recursive nature of createModelFromDescriptor
        if (model == null) {
            model = CREATING.get(descriptor.getName());
            if (model == null) {
                model = createModelFromDescriptor(context, descriptor);
                context.getFullNameToModelMap().put(descriptor.getFullName(), model);
            }
        }
        return model;
    }
    
    static Model createModelFromDescriptor(final ScslSwaggerConversionContext context, final Descriptor descriptor) {
        final ModelImpl model = new ModelImpl();
        CREATING.put(descriptor.getName(), model);
        model.setReference(descriptor.getName());
        model.setTitle(descriptor.getName());
        model.type("object");
        final String description = descriptor.getOptions().getExtension(Description.message);
        if (description != null && !description.isEmpty()) {
            model.description(description);
        }
        
        buildModelFromDescriptor(context, descriptor, model);
        
        model.getVendorExtensions().put("x-modelRef", descriptor.getFullName());
        CREATING.remove(descriptor.getName());
        return model;
    }
    
    private static void buildModelFromDescriptor(final ScslSwaggerConversionContext context, final Descriptor descriptor, final ModelImpl model) {
        for (final FieldDescriptor field : descriptor.getFields()) {
            final FieldOptions fieldOptions = field.getOptions();
            final String fieldValidation = fieldOptions.getExtension(Valid.field);
            final String valuesValidation = fieldOptions.getExtension(Valid.values);
            final String keysValidation = fieldOptions.getExtension(Valid.keys);
            
            if (field.isRepeated() && !field.isMapField()) {
                final ArrayProperty arrayProperty = getArrayProperty(context, field, fieldOptions, fieldValidation, valuesValidation);
                model.addProperty(arrayProperty.getName(), arrayProperty);
                
            } else if (field.isMapField()) {
                final MapProperty mapProperty = getMapProperty(context, field, fieldOptions, fieldValidation, valuesValidation, keysValidation);
                model.addProperty(mapProperty.getName(), mapProperty);
                
            } else {
                final Property property = convertFieldToProperty(context, field, fieldValidation);
                // uncomment this to prevent refs from having description and validation as siblings to $ref
                // if (!JavaType.MESSAGE.equals(field.getJavaType())) {
                final String descriptionField = fieldOptions.getExtension(Description.field);
                if (descriptionField != null && !descriptionField.isEmpty()) {
                    property.setDescription(descriptionField);
                }
                // Add the validation details as vendor extensions so that all validation is documented in the contract
                // some way or another
                if (fieldValidation != null && !fieldValidation.isEmpty()) {
                    property.getVendorExtensions().put("x-fieldValidation", fieldValidation);
                }
                // }
                
                model.addProperty(property.getName(), property);
            }
        }
    }
    
    private static ArrayProperty getArrayProperty(final ScslSwaggerConversionContext context,
                                                  final FieldDescriptor field,
                                                  final FieldOptions fieldOptions,
                                                  final String fieldValidation,
                                                  final String valuesValidation) {
        final ArrayProperty arrayProperty = new ArrayProperty();
        arrayProperty.setName(field.getJsonName());
        arrayProperty.title(field.getJsonName()); // TODO do we ned to set this on normal properties?
        final String descriptionField = fieldOptions.getExtension(Description.field);
        if (descriptionField != null && !descriptionField.isEmpty()) {
            arrayProperty.description(descriptionField);
        }
        final Property itemsProperty = convertFieldToProperty(context, field, valuesValidation);
        arrayProperty.setItems(itemsProperty);
        
        final ValidationDetails validationDetails = extractValidationDetails(fieldValidation);
        
        if (validationDetails.isRequired()) {
            arrayProperty.setMinItems(1);
            arrayProperty.setRequired(true);
        }
        if (validationDetails.getMinLength() > 0) {
            arrayProperty.setMinItems(validationDetails.getMinLength());
        }
        if (validationDetails.getMaxLength() > 0) {
            arrayProperty.setMaxItems(validationDetails.getMaxLength());
        }
        
        if (fieldValidation != null && !fieldValidation.isEmpty()) {
            arrayProperty.getVendorExtensions().put("x-fieldValidation", fieldValidation);
        }
        // uncomment this to place validation with array instead of items ref
        // if (valuesValidation != null && !valuesValidation.isEmpty()) {
        // arrayProperty.getVendorExtensions().put("x-valuesValidation", valuesValidation);
        // }
        return arrayProperty;
    }
    
    private static MapProperty getMapProperty(final ScslSwaggerConversionContext context,
                                              final FieldDescriptor field,
                                              final FieldOptions fieldOptions,
                                              final String fieldValidation,
                                              final String valuesValidation,
                                              final String keysValidation) {
        final MapProperty mapProperty = new MapProperty();
        mapProperty.setName(field.getJsonName());
        mapProperty.title(field.getJsonName());
        final String descriptionField = fieldOptions.getExtension(Description.field);
        if (descriptionField != null && !descriptionField.isEmpty()) {
            mapProperty.description(descriptionField);
        }
        
        final Property valuesProperty = convertFieldToProperty(context, field.getMessageType().getFields().get(1), valuesValidation);
        mapProperty.setAdditionalProperties(valuesProperty);
        
        final ValidationDetails validationDetails = extractValidationDetails(fieldValidation);
        
        if (validationDetails.isRequired()) {
            mapProperty.setMinProperties(1);
            mapProperty.setRequired(true);
        }
        if (validationDetails.getMinLength() > 0) {
            mapProperty.setMinProperties(validationDetails.getMinLength());
        }
        if (validationDetails.getMaxLength() > 0) {
            mapProperty.setMaxProperties(validationDetails.getMaxLength());
        }
        
        if (fieldValidation != null && !fieldValidation.isEmpty()) {
            mapProperty.getVendorExtensions().put("x-fieldValidation", fieldValidation);
        }
        if (keysValidation != null && !keysValidation.isEmpty()) {
            mapProperty.getVendorExtensions().put("x-keysValidation", keysValidation);
        }
        // uncomment this to place validation with map instead of items ref
        // if (valuesValidation != null && !valuesValidation.isEmpty()) {
        // mapProperty.getVendorExtensions().put("x-valuesValidation", valuesValidation);
        // }
        return mapProperty;
    }
    
    private static Property convertFieldToProperty(final ScslSwaggerConversionContext context, // NOSONAR switch statement is required
                                                   final FieldDescriptor field,
                                                   final String fieldValidation) {
        final AbstractProperty property;
        final ValidationDetails validationDetails = extractValidationDetails(fieldValidation);
        
        final JavaType javaType = field.getJavaType();
        switch (javaType) {
            case STRING: // NOSONAR this case statement is not complex
                property = new StringProperty();
                if (validationDetails.getMinLength() >= 0) {
                    ((StringProperty) property).setMinLength(validationDetails.getMinLength());
                }
                if (validationDetails.getMaxLength() >= 0) {
                    ((StringProperty) property).setMaxLength(validationDetails.getMaxLength());
                }
                if (validationDetails.getRegex() != null) {
                    ((StringProperty) property).pattern(validationDetails.getRegex());
                }
                // temporary until we have the sensitive data markers
                if (field.getName().toLowerCase().contains("password") || field.getName().toLowerCase().contains("secret")) {
                    property.setFormat("password");
                }
                
                break;
            case BOOLEAN:
                property = new BooleanProperty();
                break;
            case INT:
                property = new IntegerProperty();
                break;
            case LONG:
                property = new StringProperty("int64"); // because javascript converts long int into float
                break;
            case DOUBLE:
                property = new DoubleProperty();
                break;
            case FLOAT:
                property = new FloatProperty();
                break;
            case ENUM: // NOSONAR this case statement is not complex
                property = new StringProperty();
                final List<String> enumValues = field
                    .getEnumType()
                    .getValues()
                    .stream()
                    .map(EnumValueDescriptor::getName)
                    .collect(Collectors.toList());
                ((StringProperty) property)._enum(enumValues);
                // an extra field with the enum fully qualified name, such that validations that refer to this enum will
                // be able to resolve it
                // otherwise, this information is lost in swagger
                property.getVendorExtensions().put("x-enumRef", field.getEnumType().getFullName());
                break;
            case MESSAGE:
                property = new RefProperty();
                final Model messageModel = getOrCreateModelFromDescriptor(context, field.getMessageType());
                ((RefProperty) property).asDefault(messageModel.getReference());
                break;
            default:
                LOG.warn("Unhandled field type {} for field: {}", javaType, field.getJsonName());
                property = new StringProperty();
                // property.setName("unhandled");
                break;
        }
        
        property.setName(field.getJsonName());
        property.setRequired(validationDetails.isRequired());
        // Allow Empty value is only valid in "query" and "form-data" parameters .. otherwise it will break swagger
        // validation
        // if (validationDetails.isRequireNonEmpty()) {
        // property.setAllowEmptyValue(false);
        // }
        if (property instanceof AbstractNumericProperty) {
            if (validationDetails.getMinValue() != null) {
                ((AbstractNumericProperty) property).minimum(validationDetails.getMinValue());
            }
            if (validationDetails.getMaxValue() != null) {
                ((AbstractNumericProperty) property).maximum(validationDetails.getMaxValue());
            }
        }
        
        return property;
    }
    
    private static ValidationDetails extractValidationDetails(String fieldValidation) { // NOSONAR switch statement is required
        final ValidationDetails validationDetails = new ValidationDetails();
        
        // start of validation method calls
        Matcher m = FIELD_VALIDATION_PATTERN.matcher(fieldValidation);
        
        while (m.find()) {
            switch (m.group(1)) {
                case REQUIRED:
                    validationDetails.setRequired();
                    break;
                case HAS_TEXT:
                    validationDetails.setRequireNonEmpty();
                    break;
            }
        }
        
        // consume already matched
        fieldValidation = m.replaceAll("");
        
        m = FIELD_VALIDATION_PARAMS_PATTERN.matcher(fieldValidation);
        while (m.find()) {
            try {
                switch (m.group(1)) {
                    case SIZE: // NOSONAR this case statement is not complex
                        final List<String> sizeParams = splitAndTrimParams(m.group(2));
                        if ("_".equals(sizeParams.get(0))) {
                            if (!"_".equals(sizeParams.get(1))) {
                                validationDetails.setMaxLength(Integer.parseInt(sizeParams.get(1)));
                            }
                        } else if ("_".equals(sizeParams.get(1))) {
                            validationDetails.setMinLength(Integer.parseInt(sizeParams.get(0)));
                        } else {
                            validationDetails.setMinLength(Integer.parseInt(sizeParams.get(0)));
                            validationDetails.setMaxLength(Integer.parseInt(sizeParams.get(1)));
                        }
                        break;
                    case RANGE: // NOSONAR this case statement is not complex
                        final List<String> rangeParams = splitAndTrimParams(m.group(2));
                        if (!"_".equals(rangeParams.get(0))) {
                            validationDetails.setMinValue(new BigDecimal(rangeParams.get(0)));
                        }
                        if (!"_".equals(rangeParams.get(1))) {
                            validationDetails.setMaxValue(new BigDecimal(rangeParams.get(1)));
                        }
                        break;
                    case EXCLUSIVE_RANGE: // NOSONAR this case statement is not complex
                        final List<String> excRangeParams = splitAndTrimParams(m.group(2));
                        if (!"_".equals(excRangeParams.get(0))) {
                            validationDetails.setMinValue(new BigDecimal(excRangeParams.get(0)));
                        }
                        if (!"_".equals(excRangeParams.get(1))) {
                            validationDetails.setMaxValue(new BigDecimal(excRangeParams.get(1)));
                        }
                        break;
                    case REGEX:
                        final String pattern = m.group(2);
                        validationDetails.setRegex(pattern);
                        break;
                    default:
                        break;
                }
            } catch (final NumberFormatException e) {
                LOG.warn("Number format exception while interpreting field validation [{}]. Skipping validation.", fieldValidation);
            }
        }
        return validationDetails;
    }
    
}
