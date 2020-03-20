package com.ixaris.commons.protobuf.lib;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.StampedLock;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLite.Builder;
import com.google.protobuf.MessageLiteOrBuilder;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Parser;
import com.google.protobuf.util.JsonFormat;

import com.ixaris.commons.misc.lib.lock.LockUtil;

public final class MessageHelper {
    
    private static final Map<Class<?>, MessageLite> INSTANCES_MAP = new HashMap<>();
    private static final StampedLock INSTANCES_LOCK = new StampedLock();
    
    @SuppressWarnings("unchecked")
    public static <T extends MessageLite> T parse(final T defaultInstance, final ByteString bytes) throws InvalidProtocolBufferException {
        return ((Parser<T>) defaultInstance.getParserForType()).parseFrom(bytes);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends MessageLite> T parse(final T defaultInstance, final String json) throws InvalidProtocolBufferException {
        final Builder builder = defaultInstance.newBuilderForType();
        if (builder instanceof Message.Builder) {
            JsonFormat.parser().ignoringUnknownFields().merge(json, (Message.Builder) builder);
            return (T) builder.build();
        } else {
            throw new UnsupportedOperationException("Unsupported parsing json for " + defaultInstance.getClass());
        }
    }
    
    public static <T extends MessageLite> T parse(final Class<T> type, final ByteString bytes) throws InvalidProtocolBufferException {
        return parse(resolveInstance(type), bytes);
    }
    
    public static <T extends MessageLite> T parse(final Class<T> type, final ByteString bytes, final boolean json) throws InvalidProtocolBufferException {
        return !json ? parse(resolveInstance(type), bytes) : parse(resolveInstance(type), bytes.toStringUtf8());
    }
    
    public static <T extends MessageLite> T parse(final Class<T> type, final String json) throws InvalidProtocolBufferException {
        return parse(resolveInstance(type), json);
    }
    
    public static String json(final MessageLiteOrBuilder instance) {
        return json(instance, false);
    }
    
    public static String json(final MessageLiteOrBuilder instance, final boolean compressed) {
        if (instance == null) {
            return "";
        }
        
        if (instance instanceof MessageOrBuilder) {
            try {
                final JsonFormat.Printer printer;
                if (compressed) {
                    printer = JsonFormat.printer().omittingInsignificantWhitespace();
                } else {
                    printer = JsonFormat.printer().includingDefaultValueFields();
                }
                return printer.print((MessageOrBuilder) instance);
            } catch (final InvalidProtocolBufferException e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported printing json for " + instance.getClass().toString());
        }
    }
    
    public static ByteString bytes(final MessageLite instance, final boolean json) {
        return !json ? instance.toByteString() : ByteString.copyFromUtf8(json(instance));
    }
    
    public static long fingerprint(final MessageLiteOrBuilder instance) {
        if (instance instanceof MessageOrBuilder) {
            final MessageOrBuilder mb = (MessageOrBuilder) instance;
            long fingerprint = 0L;
            for (final Map.Entry<FieldDescriptor, Object> entry : mb.getAllFields().entrySet()) {
                final long fieldFingerprint = fieldFingerprint(entry.getKey(), entry.getValue());
                if (fieldFingerprint != 0L) { // skip default values
                    fingerprint = (53L * fingerprint) + fieldFingerprint;
                }
            }
            return fingerprint;
        } else {
            throw new UnsupportedOperationException("Unsupported fingerprint for "
                + (instance == null ? "[null message lite instance]" : instance.getClass().toString()));
        }
    }
    
    public static long fieldFingerprint(final FieldDescriptor descriptor, final Object field) {
        if (descriptor.isRepeated()) {
            final List list = (List) field;
            long fingerprint = list.size();
            for (final Object singleField : list) {
                final long fieldFingerprint = singleFieldFingerprint(descriptor.getJavaType(), singleField);
                if (fieldFingerprint != 0L) { // skip default values
                    fingerprint = (53L * fingerprint) + fieldFingerprint;
                }
            }
            return fingerprint;
        } else {
            return singleFieldFingerprint(descriptor.getJavaType(), field);
        }
    }
    
    /**
     * Calculated the fingerprint for a single field. It is important that 0 is returned for default values
     */
    public static long singleFieldFingerprint(final JavaType javaType, final Object field) { // NOSONAR
        switch (javaType) {
            case LONG:
                return (Long) field;
            case INT:
            case FLOAT:
            case DOUBLE:
                return ((Number) field).longValue();
            case BOOLEAN:
                return ((Boolean) field) ? 1L : 0L;
            case ENUM:
                return ((EnumValueDescriptor) field).getNumber();
            case STRING:
                return "".equals(field) ? 0L : field.hashCode();
            case BYTE_STRING:
                return ((ByteString) field).isEmpty() ? 0L : field.hashCode();
            case MESSAGE:
                return fingerprint((MessageLite) field);
            default:
                throw new UnsupportedOperationException("Unable to fingerprint field of type " + javaType);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends MessageLite> T resolveInstance(final Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type should not be null");
        }
        
        final MessageLite instance = LockUtil.readMaybeWrite(INSTANCES_LOCK,
            true,
            () -> INSTANCES_MAP.get(type),
            Objects::nonNull,
            () -> INSTANCES_MAP.computeIfAbsent(type, k -> {
                try {
                    return (MessageLite) k.getMethod("getDefaultInstance").invoke(null);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }));
        
        return (T) instance;
    }
    
    private MessageHelper() {}
    
}
