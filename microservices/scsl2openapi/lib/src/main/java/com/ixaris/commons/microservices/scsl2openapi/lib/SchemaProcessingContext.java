package com.ixaris.commons.microservices.scsl2openapi.lib;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.EXCLUSIVE_RANGE;
import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.FIELD_VALIDATION_PARAMS_PATTERN;
import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.FIELD_VALIDATION_PATTERN;
import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.RANGE;
import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.REGEX;
import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.REQUIRED;
import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.SIZE;
import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.splitAndTrimParams;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import com.ixaris.commons.microservices.scsl2openapi.lib.OpenAPISchemaConverter.ProcesingContext;
import com.ixaris.commons.misc.lib.object.Tuple2;

import description.Description;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.PasswordSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import valid.Valid;

public final class SchemaProcessingContext implements ProcesingContext<Schema<?>> {
    
    private static final Logger LOG = LoggerFactory.getLogger(SchemaProcessingContext.class);
    
    static final String QUALIFIED_NAME_KEY = "x-qualifiedName";
    
    private final Map<String, String> fullNameToName;
    private final Map<String, Schema<?>> fullNameToSchema = new LinkedHashMap<>();
    
    public SchemaProcessingContext(final Map<String, String> fullNameToName) {
        this.fullNameToName = fullNameToName;
    }
    
    @Override
    public Schema<?> get(final String fullName) {
        return fullNameToSchema.get(fullName);
    }
    
    public Set<Entry<String, Schema<?>>> entrySet() {
        return fullNameToSchema.entrySet();
    }
    
    @Override
    public Schema<?> processDescriptor(final Descriptor descriptor) {
        final ObjectSchema schema = new ObjectSchema();
        schema.addExtension(QUALIFIED_NAME_KEY, descriptor.getFullName());
        
        final String name = fullNameToName.get(descriptor.getFullName());
        if (name == null) {
            throw new IllegalStateException("Name not found for " + descriptor.getFullName());
        }
        schema.setName(name);
        final String description = descriptor.getOptions().getExtension(Description.message);
        if (!description.isEmpty()) {
            schema.description(description);
        }
        final List<String> messageValidations = descriptor.getOptions().getExtension(Valid.message);
        if (!messageValidations.isEmpty()) {
            schema.addExtension("x-messageValidation", String.join(" ", messageValidations));
        }
        fullNameToSchema.put(descriptor.getFullName(), schema);
        return schema;
    }
    
    @Override
    public Schema<?> processEnumDescriptor(final EnumDescriptor descriptor) {
        final StringSchema schema = new StringSchema();
        schema.addExtension(QUALIFIED_NAME_KEY, descriptor.getFullName());
        final String name = fullNameToName.get(descriptor.getFullName());
        if (name == null) {
            throw new IllegalStateException("Name not found for " + descriptor.getFullName());
        }
        schema.setName(name);
        final String description = descriptor.getOptions().getExtension(Description.enumeration);
        if (!description.isEmpty()) {
            schema.description(description);
        }
        
        final List<String> enumValues = descriptor.getValues()
            .stream()
            .map(EnumValueDescriptor::getName)
            .collect(Collectors.toList());
        schema.setEnum(enumValues);
        fullNameToSchema.put(descriptor.getFullName(), schema);
        return schema;
    }
    
    @Override
    public void processArrayDescriptor(
                                       final Schema<?> parent,
                                       final Message itemDefaultInstance,
                                       final FieldDescriptor field,
                                       final FieldOptions fieldOptions) {
        final String fieldValidation = fieldOptions.getExtension(Valid.field);
        final String valuesValidation = fieldOptions.getExtension(Valid.values);
        
        final ArraySchema schema = new ArraySchema();
        parent.addProperties(field.getJsonName(), schema);
        
        final String description = fieldOptions.getExtension(Description.field);
        if (!description.isEmpty()) {
            schema.description(description);
        }
        
        final Schema<?> itemsProperty = processField(itemDefaultInstance, field, valuesValidation).get1();
        schema.setItems(itemsProperty);
        
        final ValidationDetails validationDetails = extractValidationDetails(fieldValidation);
        
        if (validationDetails.isRequired()) {
            schema.setMinItems(1);
            parent.addRequiredItem(field.getJsonName());
        }
        if (validationDetails.getMinLength() > 0) {
            schema.setMinItems(validationDetails.getMinLength());
        }
        if (validationDetails.getMaxLength() > 0) {
            schema.setMaxItems(validationDetails.getMaxLength());
        }
        
        if (!fieldValidation.isEmpty()) {
            schema.addExtension("x-fieldValidation", fieldValidation);
        }
        if (!valuesValidation.isEmpty()) {
            schema.addExtension("x-valuesValidation", valuesValidation);
        }
    }
    
    @Override
    public void processMapDescriptor(
                                     final Schema<?> parent,
                                     final Message itemDefaultInstance,
                                     final FieldDescriptor field,
                                     final FieldOptions fieldOptions,
                                     final EnumDescriptor keysEnumeration) {
        final String fieldValidation = fieldOptions.getExtension(Valid.field);
        final String valuesValidation = fieldOptions.getExtension(Valid.values);
        final String keysValidation = fieldOptions.getExtension(Valid.keys);
        
        final MapSchema schema = new MapSchema();
        parent.addProperties(field.getJsonName(), schema);
        
        final String descriptionField = fieldOptions.getExtension(Description.field);
        if (!descriptionField.isEmpty()) {
            schema.description(descriptionField);
        }
        
        final FieldDescriptor keyType = field.getMessageType().getFields().get(0);
        switch (keyType.getJavaType()) {
            case STRING:
                if (keysEnumeration != null) {
                    final Schema<?> enumSchema = OpenAPISchemaConverter.processEnum(this, keysEnumeration);
                    final Schema keySchema = new Schema().$ref(enumSchema.getName());
                    schema.addExtension("x-key$ref", keySchema.get$ref());
                } else {
                    schema.addExtension("x-keyType", "string");
                }
                break;
            case INT:
                schema.addExtension("x-keyType", "integer");
                schema.addExtension("x-keyFormat", "int32");
                break;
            case LONG:
                schema.addExtension("x-keyType", "integer");
                schema.addExtension("x-keyFormat", "int64");
                break;
            default:
                LOG.warn("Unhandled map key type {} for field: {}", keyType.getJavaType(), field.getJsonName());
                break;
        }
        
        final Schema<?> valuesProperty = processField(
            itemDefaultInstance, field.getMessageType().getFields().get(1), valuesValidation)
                .get1();
        schema.setAdditionalProperties(valuesProperty);
        
        final ValidationDetails validationDetails = extractValidationDetails(fieldValidation);
        
        if (validationDetails.isRequired()) {
            schema.setMinProperties(1);
            parent.addRequiredItem(field.getJsonName());
        }
        if (validationDetails.getMinLength() > 0) {
            schema.setMinProperties(validationDetails.getMinLength());
        }
        if (validationDetails.getMaxLength() > 0) {
            schema.setMaxProperties(validationDetails.getMaxLength());
        }
        
        if (!fieldValidation.isEmpty()) {
            schema.addExtension("x-fieldValidation", fieldValidation);
        }
        if (!keysValidation.isEmpty()) {
            schema.addExtension("x-keysValidation", keysValidation);
        }
        if (!valuesValidation.isEmpty()) {
            schema.addExtension("x-valuesValidation", valuesValidation);
        }
    }
    
    @Override
    public void processNestedDescriptor(
                                        final Schema<?> parent,
                                        final Message defaultInstance,
                                        final FieldDescriptor field,
                                        final FieldOptions fieldOptions) {
        final String fieldValidation = fieldOptions.getExtension(Valid.field);
        
        final Tuple2<Schema<?>, Boolean> schemaAndRequired = processField(defaultInstance, field, fieldValidation);
        Schema schema = schemaAndRequired.get1();
        Schema extensions = schema;
        
        final String descriptionField = fieldOptions.getExtension(Description.field);
        if ((schema.get$ref() != null) && (!descriptionField.isEmpty() || !fieldValidation.isEmpty())) {
            extensions = new Schema();
            schema = new ComposedSchema().addAllOfItem(schema).addAllOfItem(extensions);
        }
        if (!descriptionField.isEmpty()) {
            extensions.setDescription(descriptionField);
        }
        // Add the validation details as vendor extensions so that all validation is documented in the contract
        if (!fieldValidation.isEmpty()) {
            extensions.addExtension("x-fieldValidation", fieldValidation);
        }
        
        parent.addProperties(field.getJsonName(), schema);
        if (schemaAndRequired.get2()) {
            parent.addRequiredItem(field.getJsonName());
        }
    }
    
    private Tuple2<Schema<?>, Boolean> processField(
                                                    final Message defaultInstance, final FieldDescriptor field, final String fieldValidation) {
        final Schema<?> schema;
        final ValidationDetails validationDetails = extractValidationDetails(fieldValidation);
        
        switch (field.getJavaType()) {
            case STRING: // NOSONAR this case statement is not complex
                // temporary until we have the sensitive data markers
                if (field.getName().toLowerCase().contains("password")
                    || field.getName().toLowerCase().contains("secret")) {
                    schema = new PasswordSchema();
                } else {
                    schema = new StringSchema();
                }
                if (validationDetails.getRegex() != null) {
                    String regex = validationDetails.getRegex();
                    if (!regex.startsWith("^")) {
                        regex = "^" + regex;
                    }
                    if (!regex.endsWith("$")) {
                        regex = regex + "$";
                    }
                    schema.pattern(regex);
                }
                
                break;
            case BOOLEAN:
                schema = new BooleanSchema();
                break;
            case INT:
                schema = new IntegerSchema().format("int32");
                break;
            case LONG:
                schema = new IntegerSchema().format("int64");
                break;
            case FLOAT:
                schema = new NumberSchema().format("float");
                break;
            case DOUBLE:
                schema = new NumberSchema().format("double");
                break;
            case ENUM:
                final Schema<?> enumSchema = OpenAPISchemaConverter.processEnum(this, field.getEnumType());
                schema = new Schema().$ref(enumSchema.getName());
                break;
            case MESSAGE:
                final Schema<?> messageSchema = OpenAPISchemaConverter.processDescriptor(
                    this, defaultInstance, field.getMessageType());
                schema = new Schema().$ref(messageSchema.getName());
                break;
            default:
                LOG.warn("Unhandled field type {} for field: {}", field.getJavaType(), field.getJsonName());
                schema = new StringSchema();
                break;
        }
        
        if (validationDetails.getMinLength() >= 0) {
            schema.setMinLength(validationDetails.getMinLength());
        }
        if (validationDetails.getMaxLength() >= 0) {
            schema.setMaxLength(validationDetails.getMaxLength());
        }
        if (validationDetails.getMinValue() != null) {
            schema.setMinimum(validationDetails.getMinValue());
            schema.setExclusiveMinimum(validationDetails.isMinExclusive());
        }
        if (validationDetails.getMaxValue() != null) {
            schema.setMaximum(validationDetails.getMaxValue());
            schema.setExclusiveMaximum(validationDetails.isMaxExclusive());
        }
        
        return tuple(schema, validationDetails.isRequired());
    }
    
    private static ValidationDetails extractValidationDetails(
                                                              String fieldValidation) { // NOSONAR switch statement is required
        final ValidationDetails validationDetails = new ValidationDetails();
        
        // start of validation method calls
        Matcher m = FIELD_VALIDATION_PATTERN.matcher(fieldValidation);
        
        while (m.find()) {
            switch (m.group(1)) {
                case REQUIRED:
                    validationDetails.setRequired(true);
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
                        if (!"_".equals(sizeParams.get(0))) {
                            validationDetails.setMinLength(Integer.parseInt(sizeParams.get(0)));
                        }
                        if (!"_".equals(sizeParams.get(1))) {
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
                            validationDetails.setMinInclusive(true);
                        }
                        if (!"_".equals(excRangeParams.get(1))) {
                            validationDetails.setMaxValue(new BigDecimal(excRangeParams.get(1)));
                            validationDetails.setMaxExclusive(true);
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
                LOG.warn(
                    "Number format exception while interpreting field validation [{}]. Skipping validation.",
                    fieldValidation);
            }
        }
        return validationDetails;
    }
}
