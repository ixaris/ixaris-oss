package com.ixaris.commons.protobuf.validationcodegen;

import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.concatenatePackage;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.determineJavaInnerPackage;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.determineJavaPackage;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.determineOuterClassname;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.getOuterClassName;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.getTemplate;
import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.extractFieldValidationInfoAndDependencies;
import static com.ixaris.commons.protobuf.validationcodegen.ValidationGenerator.generateSingleMessageValidation;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import com.hubspot.jinjava.Jinjava;
import com.ixaris.commons.collections.lib.DirectedAcyclicGraph;
import com.ixaris.commons.protobuf.codegen.ProtocPlugin;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import valid.Valid;

public final class CodegenMain implements ProtocPlugin {
    
    private static final PrintStream ERROR = System.err;
    
    static {
        @SuppressWarnings("squid:S1943")
        final PrintStream err = new PrintStream(new OutputStream() {
            
            @Override
            public void write(final int b) {}
            
        });
        System.setErr(err);
    }
    
    private static final Jinjava JIN_JAVA = new Jinjava();
    private static final List<File> FILES = new ArrayList<>();
    private static final Map<String, String> FQN_PROTO_ENUM_TO_JAVA = new HashMap<>();
    
    public static void main(final String... args) {
        try {
            final ExtensionRegistry registry = ExtensionRegistry.newInstance();
            Valid.registerAllExtensions(registry);
            final CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(System.in, registry);
            final CodeGeneratorResponse response = process(request);
            
            System.out.write(response.toByteArray());
        } catch (final Exception e) {
            ERROR.println("Error while generating validations: " + e.getClass().getSimpleName() + " " + e.getMessage());
            System.exit(-1);
        }
    }
    
    private static CodeGeneratorResponse process(final CodeGeneratorRequest request) {
        final CodeGeneratorResponse.Builder codeGeneratorResponse = CodeGeneratorResponse.newBuilder();
        final Set<String> toProcess = new HashSet<>(request.getFileToGenerateList());
        
        request
            .getProtoFileList()
            .forEach(fileDescriptor -> {
                extractMappings(fileDescriptor);
                
                if (toProcess.contains(fileDescriptor.getName())) {
                    parseAndGenerateValidations(fileDescriptor);
                }
            });
        
        codeGeneratorResponse.addAllFile(FILES);
        
        return codeGeneratorResponse.build();
    }
    
    private static void extractMappings(final FileDescriptorProto fileDescriptor) {
        final String protoPackage = fileDescriptor.getPackage();
        final String javaPackage = determineJavaInnerPackage(
            fileDescriptor,
            determineJavaPackage(fileDescriptor),
            determineOuterClassname(fileDescriptor, getOuterClassName(fileDescriptor))
        );
        extractEnumMappings(fileDescriptor.getEnumTypeList(), protoPackage, javaPackage);
        extractTypeMappings(fileDescriptor.getMessageTypeList(), protoPackage, javaPackage);
    }
    
    private static void extractTypeMappings(
        final List<DescriptorProto> descriptors, final String protoPackage, final String javaPackage
    ) {
        descriptors.stream()
            .filter(descriptor -> !descriptor.getOptions().getMapEntry())
            .forEach(descriptor -> {
                final String nextProtoPackage = concatenatePackage(protoPackage, descriptor.getName());
                final String nextJavaPackage = concatenatePackage(javaPackage, descriptor.getName());
                extractEnumMappings(descriptor.getEnumTypeList(), nextProtoPackage, nextJavaPackage);
                extractTypeMappings(descriptor.getNestedTypeList(), nextProtoPackage, nextJavaPackage);
            });
    }
    
    private static void extractEnumMappings(
        final List<EnumDescriptorProto> descriptors, final String protoPackage, final String javaPackage
    ) {
        descriptors.forEach(descriptor -> {
            final String nextProtoPackage = concatenatePackage(protoPackage, descriptor.getName());
            final String nextJavaPackage = concatenatePackage(javaPackage, descriptor.getName());
            FQN_PROTO_ENUM_TO_JAVA.put(nextProtoPackage, nextJavaPackage);
        });
    }
    
    private static void parseAndGenerateValidations(final FileDescriptorProto fileDescriptor) {
        final String protoPackage = fileDescriptor.getPackage();
        final String javaPackage = determineJavaPackage(fileDescriptor);
        final String outerClassName = determineOuterClassname(fileDescriptor, getOuterClassName(fileDescriptor));
        final String messagePackage = determineJavaInnerPackage(fileDescriptor, javaPackage, outerClassName);
        
        final List<String> messages = new ArrayList<>(fileDescriptor.getMessageTypeList().size());
        fileDescriptor
            .getMessageTypeList()
            .stream()
            .filter(d -> !d.getOptions().getMapEntry())
            .forEach(descriptor -> {
                final String message = renderMessage(
                    descriptor,
                    javaPackage,
                    protoPackage,
                    messagePackage,
                    fileDescriptor.getOptions().getJavaMultipleFiles() ? "" : "    ",
                    fileDescriptor.getOptions().getJavaMultipleFiles() ? "" : "static "
                );
                
                if (fileDescriptor.getOptions().getJavaMultipleFiles()) {
                    
                    final Map<String, Object> context = new HashMap<>();
                    context.put("javaPackage", javaPackage);
                    context.put("message", message);
                    
                    final String rendered = JIN_JAVA.render(getTemplate("MultiFileValidationsTemplate.jinja"), context);
                    
                    FILES.add(
                        File.newBuilder()
                            .setName(
                                (javaPackage + "." + descriptor.getName() + "Validation").replaceAll("\\.", "/")
                                    + ".java"
                            )
                            .setContent(rendered)
                            .build()
                    );
                } else {
                    messages.add(message);
                }
            });
        
        if (!messages.isEmpty()) {
            final Map<String, Object> context = new HashMap<>();
            context.put("javaPackage", javaPackage);
            context.put("className", outerClassName + "Validation");
            context.put("messages", messages);
            
            final String rendered = JIN_JAVA.render(getTemplate("SingleFileValidationsTemplate.jinja"), context);
            
            FILES.add(
                File.newBuilder()
                    .setName((javaPackage + "." + outerClassName + "Validation").replaceAll("\\.", "/") + ".java")
                    .setContent(rendered)
                    .build()
            );
        }
    }
    
    private static String renderMessage(
        final DescriptorProto descriptor,
        final String javaPackage,
        final String protoPackage,
        final String messagePackage,
        final String tab,
        final String staticStr
    ) {
        final Map<String, FieldValidationInfo> fieldValidations = new HashMap<>();
        final DirectedAcyclicGraph<String> fieldDependencies = new DirectedAcyclicGraph<>();
        final List<MessageValidationInfo> messageValidations = new ArrayList<>();
        final List<String> messageKeyEnums = new ArrayList<>();
        
        // this will extract message validations and populate fieldValidations with mentioned fields,
        // such that we at least generate v.field() for these fields (otherwise we will not know if they are present)
        extractMessageValidationInfo(descriptor, messageValidations, fieldValidations);
        
        descriptor
            .getFieldList()
            .forEach(field ->
                extractFieldValidationInfo(
                    concatenatePackage(protoPackage, descriptor.getName()),
                    descriptor,
                    field,
                    fieldValidations,
                    fieldDependencies,
                    messageKeyEnums,
                    FQN_PROTO_ENUM_TO_JAVA
                )
            );
        
        final List<String> validations = new ArrayList<>(fieldDependencies.size() + messageValidations.size());
        fieldDependencies
            .sortRootsToLeaves()
            .forEach(f -> {
                final FieldValidationInfo info = fieldValidations.get(f);
                if (info == null) {
                    throw new IllegalStateException("required field " + f + " not present");
                }
                validations.add(info.validation);
            });
        messageValidations.forEach(messageValidation -> validations.add(messageValidation.validation));
        
        final List<String> messages = new ArrayList<>(descriptor.getNestedTypeList().size());
        descriptor
            .getNestedTypeList()
            .stream()
            .filter(d -> !d.getOptions().getMapEntry())
            .forEach(nestedDescriptor -> {
                messages.add(renderMessage(
                    nestedDescriptor,
                    javaPackage,
                    protoPackage,
                    messagePackage + "." + descriptor.getName(),
                    tab + "    ",
                    "static "
                ));
            });
        
        final Map<String, Object> context = new HashMap<>();
        context.put("javaPackage", javaPackage);
        context.put("validationClassName", descriptor.getName() + "Validation");
        context.put("messageFullName", messagePackage + "." + descriptor.getName());
        context.put("tab", tab);
        context.put("static", staticStr);
        if (!messageKeyEnums.isEmpty()) {
            context.put("keyEnums", messageKeyEnums);
        }
        context.put("validations", validations);
        context.put("messages", messages);
        
        return JIN_JAVA.render(getTemplate("MessageValidationTemplate.jinja"), context);
    }
    
    private static void extractFieldValidationInfo(
        final String protoPackage,
        final DescriptorProto descriptor,
        final FieldDescriptorProto field,
        final Map<String, FieldValidationInfo> fieldValidations,
        final DirectedAcyclicGraph<String> fieldDependencies,
        final List<String> messageKeyEnums,
        final Map<String, String> fqnProtoEnumToJava
    ) {
        final boolean repeated = field.getLabel() == Label.LABEL_REPEATED;
        final JavaType javaType = FieldDescriptor.Type.valueOf(field.getType()).getJavaType();
        final String typeName = field.getTypeName();
        final DescriptorProto keyValueDescriptor;
        if (repeated && JavaType.MESSAGE.equals(javaType)) {
            keyValueDescriptor = descriptor
                .getNestedTypeList()
                .stream()
                .filter(
                    n ->
                        n.getOptions().getMapEntry()
                            && typeName.equals("." + concatenatePackage(protoPackage, n.getName()))
                )
                .findAny()
                .orElse(null);
        } else {
            keyValueDescriptor = null;
        }
        final boolean mapField = keyValueDescriptor != null;
        
        extractFieldValidationInfoAndDependencies(
            protoPackage,
            field.getName(),
            repeated,
            mapField,
            javaType,
            typeName,
            () -> FieldDescriptor.Type.valueOf(keyValueDescriptor.getField(0).getType()).getJavaType(),
            () -> FieldDescriptor.Type.valueOf(keyValueDescriptor.getField(1).getType()).getJavaType(),
            field.getOptions().getExtension(Valid.field),
            field.getOptions().getExtension(Valid.values),
            field.getOptions().getExtension(Valid.keys),
            field.getOptions().getExtension(Valid.enumeration),
            fieldValidations,
            fieldDependencies,
            messageKeyEnums,
            fqnProtoEnumToJava
        );
    }
    
    private static void extractMessageValidationInfo(
        final DescriptorProto descriptor,
        final List<MessageValidationInfo> messageValidations,
        final Map<String, FieldValidationInfo> fieldValidations
    ) {
        descriptor
            .getOptions()
            .getExtension(Valid.message)
            .stream()
            .filter(v -> !v.equals(""))
            .forEach(v -> {
                final MessageValidationInfo info = new MessageValidationInfo();
                final StringBuilder sb = new StringBuilder();
                generateSingleMessageValidation(v, sb, fieldValidations);
                if (sb.length() > 0) {
                    info.validation = sb.toString();
                    messageValidations.add(info);
                }
            });
    }
    
    private static class MessageValidationInfo {
        
        private String validation = "";
        
    }
    
    public CodegenMain() {}
    
    @Override
    public String getName() {
        return "protovalidation";
    }
    
}
