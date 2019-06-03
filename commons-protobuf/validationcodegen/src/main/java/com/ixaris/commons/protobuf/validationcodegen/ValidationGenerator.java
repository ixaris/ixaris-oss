package com.ixaris.commons.protobuf.validationcodegen;

import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.convertNameToType;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.resolveJavaMapping;

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.ixaris.commons.collections.lib.DirectedAcyclicGraph;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public final class ValidationGenerator {
    
    public static final String REQUIRED = "required";
    public static final String HAS_TEXT = "has_text";
    
    public static final String REQUIRES = "requires";
    public static final String SIZE = "size";
    public static final String RANGE = "range";
    public static final String EXCLUSIVE_RANGE = "exc_range";
    
    public static final String IN = "in";
    public static final String NOT_IN = "not_in";
    public static final String REGEX = "regex";
    
    public static final Pattern FIELD_VALIDATION_PATTERN = Pattern.compile(
        "(?:^| +)(" + REQUIRED + "|" + HAS_TEXT + ")(?=$| +)"
    );
    public static final Pattern FIELD_VALIDATION_PARAMS_PATTERN = Pattern.compile(
        "(?:^| +)("
            + REQUIRES
            + "|"
            + SIZE
            + "|"
            + RANGE
            + "|"
            + EXCLUSIVE_RANGE
            + "|"
            + IN
            + "|"
            + NOT_IN
            + "|"
            + REGEX
            + ")(?: *)\\((.*?)\\)(?=$| +)"
    );
    
    private static final String EXACTLY = "exactly";
    private static final String AT_LEAST = "at_least";
    private static final String AT_MOST = "at_most";
    private static final String ALL_OR_NONE = "all_or_none";
    
    private static final Pattern MESSAGE_VALIDATION_PATTERN = Pattern.compile(
        "(?:^| +)(" + EXACTLY + "|" + AT_LEAST + "|" + AT_MOST + "|" + ALL_OR_NONE + ")(?: *)\\((.*?)\\)(?=$| +)$"
    );
    
    static void extractFieldValidationInfoAndDependencies(
        final String protoPackage,
        final String name,
        final boolean repeated,
        final boolean mapField,
        final JavaType javaType,
        final String typeName,
        final Supplier<JavaType> keyJavaTypeSupplier,
        final Supplier<JavaType> valueJavaTypeSupplier,
        final String fieldValidation,
        final String valuesValidation,
        final String keysValidation,
        final String keysEnumeration,
        final Map<String, FieldValidationInfo> fieldValidations,
        final DirectedAcyclicGraph<String> fieldDependencies,
        final List<String> messageKeyEnums,
        final Map<String, String> fqnProtoEnumToJava
    ) {
        final FieldValidationInfo info = determineFieldTypeInfo(
            repeated, mapField, javaType, keyJavaTypeSupplier, valueJavaTypeSupplier
        );
        
        // field declaration method call
        boolean messageType = false;
        final StringBuilder sb = new StringBuilder("v.");
        switch (info.type) {
            case LIST:
                if (!keysValidation.isEmpty()) {
                    throw new IllegalArgumentException(name + ": keys validation only supported for map fields");
                }
                if (!keysEnumeration.isEmpty()) {
                    throw new IllegalArgumentException(
                        name + ": keys enumeration only supported for map fields with string keys"
                    );
                }
                sb.append("list");
                switch (info.valuesType) {
                    case BOOLEAN:
                        sb.append("Boolean");
                        break;
                    case INT:
                        sb.append("Int");
                        break;
                    case LONG:
                        sb.append("Long");
                        break;
                    case FLOAT:
                        sb.append("Float");
                        break;
                    case DOUBLE:
                        sb.append("Double");
                        break;
                    case STRING:
                        sb.append("String");
                        break;
                    case ENUM:
                        messageType = true;
                        sb.append("Enum");
                        break;
                    case MESSAGE:
                        messageType = true;
                        sb.append("Message");
                        break;
                    case BYTES:
                        sb.append("Bytes");
                        break;
                }
                break;
            case MAP:
                sb.append("map");
                switch (info.keyType) {
                    case INT:
                        if (!keysEnumeration.isEmpty()) {
                            throw new IllegalArgumentException(
                                name + ": keys enumeration only supported for map fields with string keys"
                            );
                        }
                        sb.append("Int");
                        break;
                    case LONG:
                        if (!keysEnumeration.isEmpty()) {
                            throw new IllegalArgumentException(
                                name + ": keys enumeration only supported for map fields with string keys"
                            );
                        }
                        sb.append("Long");
                        break;
                    case STRING:
                        sb.append(keysEnumeration.isEmpty() ? "String" : "Enum");
                        break;
                }
                switch (info.valuesType) {
                    case BOOLEAN:
                        sb.append("Boolean");
                        break;
                    case INT:
                        sb.append("Int");
                        break;
                    case LONG:
                        sb.append("Long");
                        break;
                    case FLOAT:
                        sb.append("Float");
                        break;
                    case DOUBLE:
                        sb.append("Double");
                        break;
                    case STRING:
                        sb.append("String");
                        break;
                    case ENUM:
                        messageType = true;
                        sb.append("Enum");
                        break;
                    case MESSAGE:
                        messageType = true;
                        sb.append("Message");
                        break;
                    case BYTES:
                        sb.append("Bytes");
                        break;
                }
                break;
            case ENUM:
            case MESSAGE:
                messageType = true;
                // fallthrough
            default:
                if (!valuesValidation.isEmpty()) {
                    throw new IllegalArgumentException(
                        name + ": values validation only supported for repeated or map fields"
                    );
                }
                if (!keysValidation.isEmpty()) {
                    throw new IllegalArgumentException(name + ": keys validation only supported for map fields");
                }
                if (!keysEnumeration.isEmpty()) {
                    throw new IllegalArgumentException(
                        name + ": keys enumeration only supported for map fields with string keys"
                    );
                }
                sb.append("field");
        }
        sb.append("(\"").append(name).append("\", ");
        if (!keysEnumeration.isEmpty()) {
            final String enumType = resolveJavaEnum(keysEnumeration, protoPackage, fqnProtoEnumToJava);
            messageKeyEnums.add("case \"" + name + "\": return getDescriptorForType(" + enumType + ".class)");
            sb.append(enumType).append(".class, ");
        }
        sb.append("m.get").append(convertNameToType(name));
        switch (info.type) {
            case LIST:
                sb.append("List");
                break;
            case MAP:
                sb.append("Map");
                break;
        }
        sb.append("())");
        
        if (
            fieldValidations.containsKey(name)
                || !fieldValidation.isEmpty()
                || !valuesValidation.isEmpty()
                || !keysValidation.isEmpty()
                || !keysEnumeration.isEmpty()
                || messageType
        ) {
            fieldDependencies.addVertex(name);
            
            generateSingleFieldValidation(
                protoPackage, name, info.type, typeName, fieldValidation, sb, fieldDependencies, fqnProtoEnumToJava
            );
            
            switch (info.type) {
                case LIST:
                    if (!valuesValidation.isEmpty()) {
                        sb.append(".values(i -> { i");
                        generateSingleFieldValidation(
                            protoPackage,
                            name,
                            info.valuesType,
                            typeName,
                            valuesValidation,
                            sb,
                            fieldDependencies,
                            fqnProtoEnumToJava
                        );
                        sb.append("; })");
                    } else if (messageType) {
                        sb.append(".values(i -> {})");
                    }
                    break;
                case MAP:
                    if (!valuesValidation.isEmpty() || !keysValidation.isEmpty() || !keysEnumeration.isEmpty()) {
                        sb.append(".entries((k, i) -> { ");
                        if (!keysValidation.isEmpty()) {
                            sb.append("k");
                            generateSingleFieldValidation(
                                protoPackage,
                                name,
                                info.keyType,
                                typeName,
                                keysValidation,
                                sb,
                                fieldDependencies,
                                fqnProtoEnumToJava
                            );
                            sb.append("; ");
                        }
                        if (!valuesValidation.isEmpty()) {
                            sb.append("i");
                            generateSingleFieldValidation(
                                protoPackage,
                                name,
                                info.valuesType,
                                typeName,
                                valuesValidation,
                                sb,
                                fieldDependencies,
                                fqnProtoEnumToJava
                            );
                            sb.append("; ");
                        }
                        sb.append("})");
                    } else if (messageType) {
                        sb.append(".entries((k, i) -> {})");
                    }
                    break;
            }
        }
        
        info.validation = sb.toString();
        fieldValidations.put(name, info);
    }
    
    static void generateSingleFieldValidation(
        final String protoPackage,
        final String name,
        final Type type,
        final String typeName,
        final String fieldValidation,
        //        final boolean key,
        final StringBuilder sb,
        final DirectedAcyclicGraph<String> fieldDependencies,
        final Map<String, String> fqnProtoEnumToJava
    ) {
        
        String validation = fieldValidation;
        
        // start of validation method calls
        Matcher m = FIELD_VALIDATION_PATTERN.matcher(validation);
        while (m.find()) {
            switch (m.group(1)) {
                case REQUIRED:
                    sb.append(".required()");
                    break;
                case HAS_TEXT:
                    if (type != Type.STRING) {
                        throw new IllegalArgumentException(name + ": has_text applicable to STRING, not " + type);
                    }
                    sb.append(".hasText()");
                    break;
            }
        }
        
        // consume already matched
        validation = m.replaceAll("");
        
        m = FIELD_VALIDATION_PARAMS_PATTERN.matcher(validation);
        while (m.find()) {
            switch (m.group(1)) {
                case REQUIRES:
                    final List<String> requiresParams = splitAndTrimParams(m.group(2));
                    sb
                        .append(".requires(")
                        .append(
                            requiresParams.stream()
                                .map(p -> {
                                    fieldDependencies.addEdge(p, name);
                                    return "\"" + p + "\"";
                                })
                                .collect(Collectors.joining(", "))
                        )
                        .append(")");
                    break;
                case SIZE:
                    if (type != Type.STRING && type != Type.LIST && type != Type.MAP) {
                        throw new IllegalArgumentException(
                            name + ": size applicable to STRING, LIST and MAP, not " + type
                        );
                    }
                    final List<String> sizeParams = splitAndTrimParams(m.group(2));
                    if (sizeParams.size() != 2) {
                        throw new IllegalArgumentException(name + ": invalid size");
                    }
                    sb.append(".size(");
                    if (sizeParams.get(0).equals("_")) {
                        if (sizeParams.get(1).equals("_")) {
                            throw new IllegalArgumentException(name + ": invalid size");
                        }
                        sb.append("null");
                    } else {
                        sb.append(Integer.parseInt(sizeParams.get(0)));
                    }
                    sb.append(", ");
                    if (sizeParams.get(1).equals("_")) {
                        sb.append("null");
                    } else {
                        sb.append(Integer.parseInt(sizeParams.get(1)));
                    }
                    sb.append(")");
                    break;
                case RANGE:
                    if (type != Type.INT && type != Type.LONG && type != Type.FLOAT && type != Type.DOUBLE) {
                        throw new IllegalArgumentException(
                            name + ": range applicable to INT, LONG, FLOAT and DOUBLE, not " + type
                        );
                    }
                    final List<String> rangeParams = splitAndTrimParams(m.group(2));
                    if (rangeParams.size() != 2) {
                        throw new IllegalArgumentException(name + ": invalid range");
                    }
                    sb.append(".range(");
                    if (rangeParams.get(0).equals("_")) {
                        if (rangeParams.get(1).equals("_")) {
                            throw new IllegalArgumentException(name + ": invalid range");
                        }
                        sb.append("(Number) null");
                    } else {
                        try {
                            sb.append(new BigDecimal(rangeParams.get(0)));
                        } catch (final NumberFormatException e) {
                            fieldDependencies.addEdge(rangeParams.get(0), name);
                            sb.append("\"").append(rangeParams.get(0)).append("\"");
                        }
                    }
                    sb.append(", ");
                    if (rangeParams.get(1).equals("_")) {
                        sb.append("(Number) null");
                    } else {
                        try {
                            sb.append(new BigDecimal(rangeParams.get(1)));
                        } catch (final NumberFormatException e) {
                            fieldDependencies.addEdge(rangeParams.get(1), name);
                            sb.append("\"").append(rangeParams.get(1)).append("\"");
                        }
                    }
                    sb.append(", false)");
                    break;
                case EXCLUSIVE_RANGE:
                    if (type != Type.INT && type != Type.LONG && type != Type.FLOAT && type != Type.DOUBLE) {
                        throw new IllegalArgumentException(
                            name + ": range applicable to INT, LONG, FLOAT and DOUBLE, not " + type
                        );
                    }
                    final List<String> excRangeParams = splitAndTrimParams(m.group(2));
                    if (excRangeParams.size() != 2) {
                        throw new IllegalArgumentException(name + ": invalid range");
                    }
                    sb.append(".range(");
                    if (excRangeParams.get(0).equals("_")) {
                        if (excRangeParams.get(1).equals("_")) {
                            throw new IllegalArgumentException(name + ": invalid range");
                        }
                        sb.append("(Number) null");
                    } else {
                        try {
                            sb.append(new BigDecimal(excRangeParams.get(0)));
                        } catch (final NumberFormatException e) {
                            fieldDependencies.addEdge(excRangeParams.get(0), name);
                            sb.append("\"").append(excRangeParams.get(0)).append("\"");
                        }
                    }
                    sb.append(", ");
                    if (excRangeParams.get(1).equals("_")) {
                        sb.append("(Number) null");
                    } else {
                        try {
                            sb.append(new BigDecimal(excRangeParams.get(1)));
                        } catch (final NumberFormatException e) {
                            fieldDependencies.addEdge(excRangeParams.get(1), name);
                            sb.append("\"").append(excRangeParams.get(1)).append("\"");
                        }
                    }
                    sb.append(", true)");
                    break;
                case IN:
                    final List<String> inParams = splitAndTrimParams(m.group(2));
                    switch (type) {
                        case INT:
                        case LONG:
                        case FLOAT:
                        case DOUBLE:
                            sb
                                .append(".in(")
                                .append(
                                    inParams.stream()
                                        .map(p -> new BigDecimal(p).toString())
                                        .collect(Collectors.joining(", "))
                                )
                                .append(")");
                            break;
                        case STRING:
                            sb
                                .append(".in(")
                                .append(
                                    inParams.stream()
                                        .map(p -> "\"" + p.substring(1, p.length() - 1) + "\"")
                                        .collect(Collectors.joining(", "))
                                )
                                .append(")");
                            break;
                        case ENUM:
                            final String enumType = resolveJavaEnum(typeName, protoPackage, fqnProtoEnumToJava);
                            sb
                                .append(".in(")
                                .append(
                                    inParams.stream().map(p -> enumType + "." + p).collect(Collectors.joining(", "))
                                )
                                .append(")");
                            break;
                        default:
                            throw new IllegalArgumentException(
                                name + ": in applicable to INT, LONG, FLOAT, DOUBLE, STRING and ENUM, not " + type
                            );
                    }
                    break;
                case NOT_IN:
                    final List<String> notInParams = splitAndTrimParams(m.group(2));
                    switch (type) {
                        case INT:
                        case LONG:
                        case FLOAT:
                        case DOUBLE:
                            sb
                                .append(".notIn(")
                                .append(
                                    notInParams.stream()
                                        .map(p -> new BigDecimal(p).toString())
                                        .collect(Collectors.joining(", "))
                                )
                                .append(")");
                            break;
                        case STRING:
                            if ((notInParams.size() == 1) && !notInParams.get(0).startsWith("'")) {
                                // enum
                                sb
                                    .append(".notIn(")
                                    .append(resolveJavaEnum(notInParams.get(0), protoPackage, fqnProtoEnumToJava))
                                    .append(".class)");
                            } else {
                                sb
                                    .append(".notIn(")
                                    .append(
                                        notInParams.stream()
                                            .map(p -> "\"" + p.substring(1, p.length() - 1) + "\"")
                                            .collect(Collectors.joining(", "))
                                    )
                                    .append(")");
                            }
                            break;
                        case ENUM:
                            final String enumType = resolveJavaEnum(typeName, protoPackage, fqnProtoEnumToJava);
                            sb
                                .append(".notIn(")
                                .append(
                                    notInParams.stream().map(p -> enumType + "." + p).collect(Collectors.joining(", "))
                                )
                                .append(")");
                            break;
                        default:
                            throw new IllegalArgumentException(
                                name + ": not_in applicable to INT, LONG, FLOAT, DOUBLE, STRING and ENUM, not " + type
                            );
                    }
                    break;
                case REGEX:
                    if (type != Type.STRING) {
                        throw new IllegalArgumentException(name + ": regex applicable to STRING, not " + type);
                    }
                    try {
                        Pattern.compile(m.group(2)); // check that the pattern is correct
                    } catch (final PatternSyntaxException e) {
                        throw new IllegalArgumentException(name + ": regex syntax error " + e.getMessage(), e);
                    }
                    final String pattern = m.group(2).replaceAll("\\\\", "\\\\\\\\");
                    sb.append(".regex(\"").append(pattern).append("\")");
                    break;
            }
        }
        
        // consume already matched
        validation = m.replaceAll("");
        
        if (!validation.trim().isEmpty()) {
            throw new IllegalArgumentException(name + ": invalid validation [" + validation + "]");
        }
    }
    
    private static FieldValidationInfo determineFieldTypeInfo(
        final boolean repeated,
        final boolean mapField,
        final JavaType javaType,
        final Supplier<JavaType> keyJavaTypeSupplier,
        final Supplier<JavaType> valueJavaTypeSupplier
    ) {
        final FieldValidationInfo info = new FieldValidationInfo();
        if (repeated) {
            info.type = Type.LIST;
        }
        switch (javaType) {
            case DOUBLE: // NOSONAR this case statement is not complex
                if (info.type == null) {
                    info.type = Type.DOUBLE;
                } else {
                    info.valuesType = Type.DOUBLE;
                }
                break;
            case FLOAT: // NOSONAR this case statement is not complex
                if (info.type == null) {
                    info.type = Type.FLOAT;
                } else {
                    info.valuesType = Type.FLOAT;
                }
                break;
            case LONG: // NOSONAR this case statement is not complex
                if (info.type == null) {
                    info.type = Type.LONG;
                } else {
                    info.valuesType = Type.LONG;
                }
                break;
            case INT: // NOSONAR this case statement is not complex
                if (info.type == null) {
                    info.type = Type.INT;
                } else {
                    info.valuesType = Type.INT;
                }
                break;
            case BOOLEAN: // NOSONAR this case statement is not complex
                if (info.type == null) {
                    info.type = Type.BOOLEAN;
                } else {
                    info.valuesType = Type.BOOLEAN;
                }
                break;
            case STRING: // NOSONAR this case statement is not complex
                if (info.type == null) {
                    info.type = Type.STRING;
                } else {
                    info.valuesType = Type.STRING;
                }
                break;
            case MESSAGE:
                if (info.type == null) {
                    info.type = Type.MESSAGE;
                } else {
                    // check if this is a map
                    if (!mapField) {
                        info.valuesType = Type.MESSAGE;
                    } else {
                        info.type = Type.MAP;
                        // key type
                        final JavaType keyJavaType = keyJavaTypeSupplier.get();
                        switch (keyJavaType) {
                            case LONG:
                                info.keyType = Type.LONG;
                                break;
                            case INT:
                                info.keyType = Type.INT;
                                break;
                            case STRING:
                                info.keyType = Type.STRING;
                                break;
                            default:
                                throw new IllegalArgumentException("Unsupported key type " + keyJavaType);
                        }
                        final JavaType valueJavaType = valueJavaTypeSupplier.get();
                        switch (valueJavaType) {
                            case DOUBLE:
                                info.valuesType = Type.DOUBLE;
                                break;
                            case FLOAT:
                                info.valuesType = Type.FLOAT;
                                break;
                            case LONG:
                                info.valuesType = Type.LONG;
                                break;
                            case INT:
                                info.valuesType = Type.INT;
                                break;
                            case BOOLEAN:
                                info.valuesType = Type.BOOLEAN;
                                break;
                            case STRING:
                                info.valuesType = Type.STRING;
                                break;
                            case MESSAGE:
                                info.valuesType = Type.MESSAGE;
                                break;
                            case BYTE_STRING:
                                info.valuesType = Type.BYTES;
                                break;
                            case ENUM:
                                info.valuesType = Type.ENUM;
                                break;
                            default:
                                throw new IllegalArgumentException("Unsupported key type " + valueJavaType);
                        }
                    }
                }
                break;
            case BYTE_STRING: // NOSONAR this case statement is not complex
                if (info.type == null) {
                    info.type = Type.BYTES;
                } else {
                    info.valuesType = Type.BYTES;
                }
                break;
            case ENUM: // NOSONAR this case statement is not complex
                if (info.type == null) {
                    info.type = Type.ENUM;
                } else {
                    info.valuesType = Type.ENUM;
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported type " + javaType);
        }
        
        return info;
    }
    
    static void generateSingleMessageValidation(
        final String messageValidation, final StringBuilder sb, final Map<String, FieldValidationInfo> fieldValidations
    ) {
        final Matcher m = MESSAGE_VALIDATION_PATTERN.matcher(messageValidation);
        while (m.find()) {
            switch (m.group(1)) {
                case EXACTLY:
                    createAndAddMessageValidationInfo("exactlyN", true, m.group(2), sb, fieldValidations);
                    break;
                case AT_LEAST:
                    createAndAddMessageValidationInfo("atLeastN", true, m.group(2), sb, fieldValidations);
                    break;
                case AT_MOST:
                    createAndAddMessageValidationInfo("atMostN", true, m.group(2), sb, fieldValidations);
                    break;
                case ALL_OR_NONE:
                    createAndAddMessageValidationInfo("allOrNone", false, m.group(2), sb, fieldValidations);
                    break;
            }
        }
    }
    
    private static void createAndAddMessageValidationInfo(
        final String valMethod,
        final boolean intFirstParam,
        final String params,
        final StringBuilder sb,
        final Map<String, FieldValidationInfo> fieldValidations
    ) {
        
        sb.append("v.").append(valMethod).append("(");
        final List<String> splitParams = splitAndTrimParams(params);
        if (intFirstParam) {
            sb.append(Integer.parseInt(splitParams.get(0))).append(", ");
        }
        sb
            .append(
                splitParams.stream()
                    .skip(intFirstParam ? 1 : 0)
                    .map(p -> {
                        fieldValidations.put(p, new FieldValidationInfo());
                        return "\"" + p + "\"";
                    })
                    .collect(Collectors.joining(", "))
            )
            .append(")");
    }
    
    public static List<String> splitAndTrimParams(final String params) {
        return splitAndTrimParams(params, -1);
    }
    
    private static List<String> splitAndTrimParams(final String params, final int limit) {
        return Arrays.stream(params.split(",", limit)).map(String::trim).collect(Collectors.toList());
    }
    
    private static String resolveJavaEnum(
        final String type, final String protoPackage, final Map<String, String> fqnProtoEnumToJava
    ) {
        final String mapping = resolveJavaMapping(type, protoPackage, fqnProtoEnumToJava);
        if (mapping == null) {
            throw new IllegalStateException("Unable to resolve " + type);
        }
        return mapping;
    }
    
    private ValidationGenerator() {}
    
}
