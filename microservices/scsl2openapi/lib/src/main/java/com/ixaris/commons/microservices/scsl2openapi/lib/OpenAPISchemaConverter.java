package com.ixaris.commons.microservices.scsl2openapi.lib;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import com.ixaris.commons.misc.lib.object.GenericsUtil;

import valid.Valid;

/**
 * Utilities to convert from Protobuf Classes to Swagger Models
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
final class OpenAPISchemaConverter {
    
    private OpenAPISchemaConverter() {}
    
    interface ProcesingContext<T> {
        
        T get(String fullName);
        
        T processDescriptor(Descriptor descriptor);
        
        T processEnumDescriptor(EnumDescriptor descriptor);
        
        void processArrayDescriptor(T parent, Message itemDefaultInstance, FieldDescriptor field, FieldOptions fieldOptions);
        
        void processMapDescriptor(final T parent,
                                  final Message itemDefaultInstance,
                                  final FieldDescriptor field,
                                  final FieldOptions fieldOptions,
                                  final EnumDescriptor keysEnumeration);
        
        void processNestedDescriptor(final T parent, final Message defaultInstance, final FieldDescriptor field, final FieldOptions fieldOptions);
        
    }
    
    static <T> T processClass(final ProcesingContext<T> context, final Class<?> protobufClazz) {
        final Message defaultInstance = getDefaultInstance(protobufClazz);
        if (defaultInstance == null) {
            throw new IllegalStateException("No default instance found for " + protobufClazz);
        }
        return processDescriptor(context, defaultInstance);
    }
    
    static <T> T processDescriptor(final ProcesingContext<T> context, final Message defaultInstance) {
        return processDescriptor(context, defaultInstance, defaultInstance.getDescriptorForType());
    }
    
    static <T> T processDescriptor(final ProcesingContext<T> context, final Message defaultInstance, final Descriptor descriptor) {
        T processed = context.get(descriptor.getFullName());
        // do NOT convert the below to computeIfAbsent because LinkedHashMap does not behave correctly with nested
        // computeIfAbsents, which can happen due to recursive nature of createSchemaFromDescriptor
        if (processed != null) {
            return processed;
        }
        
        processed = context.processDescriptor(descriptor);
        
        for (final FieldDescriptor field : descriptor.getFields()) {
            final FieldOptions fieldOptions = field.getOptions();
            final EnumDescriptor keysEnumeration;
            if (!fieldOptions.getExtension(Valid.enumeration).isEmpty()) {
                // get by reflection
                try {
                    final Class<?> validation = defaultInstance
                        .getClass()
                        .getClassLoader()
                        .loadClass(defaultInstance.getClass().getName().replace("$", "Validation$") + "Validation");
                    keysEnumeration = (EnumDescriptor) validation.getMethod("getKeyEnumDescriptor", String.class).invoke(null, field.getName());
                } catch (final ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                keysEnumeration = null;
            }
            
            if (field.isRepeated() && !field.isMapField()) {
                final Type listType = getReturnType(field, "List", defaultInstance);
                final Message itemDefaultInstance;
                if (listType instanceof ParameterizedType) {
                    itemDefaultInstance = getDefaultInstance(GenericsUtil.resolveGenericTypeArgument(((ParameterizedType) listType)
                        .getActualTypeArguments()[0]));
                } else {
                    itemDefaultInstance = null;
                }
                context.processArrayDescriptor(processed, itemDefaultInstance, field, fieldOptions);
                
            } else if (field.isMapField()) {
                final Type mapType = getReturnType(field, "Map", defaultInstance);
                final Message itemDefaultInstance;
                if (mapType instanceof ParameterizedType) {
                    itemDefaultInstance = getDefaultInstance(GenericsUtil.resolveGenericTypeArgument(((ParameterizedType) mapType)
                        .getActualTypeArguments()[1]));
                } else {
                    itemDefaultInstance = null;
                }
                context.processMapDescriptor(processed, itemDefaultInstance, field, fieldOptions, keysEnumeration);
                
            } else {
                final Message nestedDefaultInstance = getDefaultInstance(GenericsUtil.resolveGenericTypeArgument(getReturnType(field, "", defaultInstance)));
                context.processNestedDescriptor(processed, nestedDefaultInstance, field, fieldOptions);
            }
        }
        
        return processed;
    }
    
    static <T> T processEnum(final ProcesingContext<T> context, final EnumDescriptor descriptor) {
        T processed = context.get(descriptor.getFullName());
        // do NOT convert the below to computeIfAbsent because LinkedHashMap does not behave correctly with nested
        // computeIfAbsents
        // which can happen due to recursive nature of createSchemaFromDescriptor
        if (processed != null) {
            return processed;
        }
        
        return context.processEnumDescriptor(descriptor);
    }
    
    static String extractDescriptorName(final Descriptor descriptor) {
        final StringBuilder name = new StringBuilder();
        Descriptor parent = descriptor;
        while (parent != null) {
            name.insert(0, parent.getName());
            parent = parent.getContainingType();
        }
        return name.toString();
    }
    
    static Message getDefaultInstance(final Class<?> type) {
        if (Message.class.isAssignableFrom(type)) {
            try {
                return (Message) type.getMethod("getDefaultInstance").invoke(null);
            } catch (final ReflectiveOperationException e) {
                throw new IllegalStateException("Invalid request type. Unable to extract swagger model.", e);
            }
        } else {
            return null;
        }
    }
    
    static Type getReturnType(final FieldDescriptor field, final String suffix, final Message defaultInstance) {
        final String jsonName = field.getJsonName();
        final String methodName = "get" + Character.toUpperCase(jsonName.charAt(0)) + jsonName.substring(1) + suffix;
        try {
            return defaultInstance.getClass().getMethod(methodName).getGenericReturnType();
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
