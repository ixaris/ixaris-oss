package com.ixaris.commons.microservices.scsl2openapi.lib;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING;
import static com.ixaris.commons.microservices.scsl2openapi.lib.OpenAPISchemaConverter.extractDescriptorName;
import static com.ixaris.commons.microservices.scsl2openapi.lib.OpenAPISchemaConverter.getDefaultInstance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ClientInvalidRequest;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.DefaultError;
import com.ixaris.commons.microservices.scsl2openapi.lib.OpenAPISchemaConverter.ProcesingContext;

import valid.Valid.MessageValidation;

public final class SchemaNameProcessingContext implements ProcesingContext<Boolean> {
    
    private final Set<String> seen = new HashSet<>();
    private final Map<String, Set<String>> schemasByName = new HashMap<>();
    
    public SchemaNameProcessingContext() {
        OpenAPISchemaConverter.processDescriptor(this, getDefaultInstance(MessageValidation.class));
        OpenAPISchemaConverter.processDescriptor(this, getDefaultInstance(ClientInvalidRequest.class));
        OpenAPISchemaConverter.processDescriptor(this, getDefaultInstance(DefaultError.class));
    }
    
    @Override
    public Boolean get(final String fullName) {
        return seen.contains(fullName) ? Boolean.TRUE : null;
    }
    
    @Override
    public Boolean processDescriptor(final Descriptor descriptor) {
        addSchema(descriptor);
        return Boolean.TRUE;
    }
    
    @Override
    public Boolean processEnumDescriptor(final EnumDescriptor descriptor) {
        addSchema(descriptor);
        return Boolean.TRUE;
    }
    
    @Override
    public void processArrayDescriptor(
                                       final Boolean parent,
                                       final Message itemDefaultInstance,
                                       final FieldDescriptor field,
                                       final FieldOptions fieldOptions) {
        processField(itemDefaultInstance, field);
    }
    
    @Override
    public void processMapDescriptor(
                                     final Boolean parent,
                                     final Message itemDefaultInstance,
                                     final FieldDescriptor field,
                                     final FieldOptions fieldOptions,
                                     final EnumDescriptor keysEnumeration) {
        final FieldDescriptor keyType = field.getMessageType().getFields().get(0);
        if ((keyType.getJavaType() == STRING) && (keysEnumeration != null)) {
            OpenAPISchemaConverter.processEnum(this, keysEnumeration);
        }
        
        processField(itemDefaultInstance, field.getMessageType().getFields().get(1));
    }
    
    @Override
    public void processNestedDescriptor(
                                        final Boolean parent,
                                        final Message defaultInstance,
                                        final FieldDescriptor field,
                                        final FieldOptions fieldOptions) {
        processField(defaultInstance, field);
    }
    
    private void processField(final Message defaultInstance, final FieldDescriptor field) {
        switch (field.getJavaType()) {
            case ENUM:
                OpenAPISchemaConverter.processEnum(this, field.getEnumType());
                break;
            case MESSAGE:
                OpenAPISchemaConverter.processDescriptor(this, defaultInstance, field.getMessageType());
                break;
        }
    }
    
    private void addSchema(final Descriptor descriptor) {
        final String fullName = descriptor.getFullName();
        final String name = extractDescriptorName(descriptor);
        schemasByName.compute(name, (k, v) -> {
            if (v == null) {
                v = new HashSet<>();
            }
            v.add(fullName);
            return v;
        });
        seen.add(fullName);
    }
    
    private void addSchema(final EnumDescriptor descriptor) {
        final String fullName = descriptor.getFullName();
        final String name = extractDescriptorName(descriptor.getContainingType()) + descriptor.getName();
        schemasByName.compute(name, (k, v) -> {
            if (v == null) {
                v = new HashSet<>();
            }
            v.add(fullName);
            return v;
        });
        seen.add(fullName);
    }
    
    public Map<String, String> dedup() {
        final Map<String, String> fullNameToName = new HashMap<>();
        for (final Entry<String, Set<String>> entry : schemasByName.entrySet()) {
            if (entry.getValue().size() > 1) {
                int i = 0;
                for (final String d : entry.getValue()) {
                    fullNameToName.put(d, entry.getKey() + i++);
                }
            } else {
                final String d = entry.getValue().iterator().next(); // get the only entry
                fullNameToName.put(d, entry.getKey());
            }
        }
        return fullNameToName;
    }
}
