package com.ixaris.commons.protobuf.lib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.MapEntry;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;

import security.Security;
import sensitive.Sensitive;
import sensitive.Sensitive.SensitiveDataLifetime;

/**
 * Helper class for messages with sensitive fields that may need to be mapped to some other value e.g. masking/tokenisation
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class SensitiveMessageHelper {
    
    private static final Set<Class<?>> BUILDERS_WITH_NO_SENSITIVE_FIELDS = new HashSet<>();
    
    private SensitiveMessageHelper() {}
    
    /**
     * Given a field, resolve the sensitive lifetime of the field. If the field is not sensitive, an empty optional is returned
     *
     * @param fieldDescriptor The protobuf field descriptor
     * @return Optional lifetime if sensitive, empty if not sensitive
     */
    public static Optional<SensitiveDataLifetime> resolveSensitiveData(final FieldDescriptor fieldDescriptor) {
        final SensitiveDataLifetime explicitLifetime = fieldDescriptor.getOptions().getExtension(Sensitive.field);
        if (explicitLifetime != null && explicitLifetime != SensitiveDataLifetime.NONE) {
            return Optional.of(explicitLifetime);
        }
        
        final Boolean sensitiveExtension = fieldDescriptor.getOptions().getExtension(Security.sensitive);
        if (sensitiveExtension != null && sensitiveExtension) {
            return Optional.of(SensitiveDataLifetime.TEMPORARY);
        }
        
        return Optional.empty();
    }
    
    /**
     * Apply mappings on sensitive data where the mapping is resolved as a promise
     *
     * @param builder The message builder to transform
     * @param temporaryDataMapper the mapping function of the values
     * @param permanentDataMapper the mapping function of the values
     * @return Updated builder with the mapper applied to all sensitive fields
     */
    public static <T extends Builder> T mapSensitiveFields(final T builder,
                                                           final SensitiveDataFunction temporaryDataMapper,
                                                           final SensitiveDataFunction permanentDataMapper) {
        return mapSensitiveFields(builder, temporaryDataMapper, permanentDataMapper, d -> d);
    }
    
    /**
     * Apply mappings on sensitive data where the mapping is resolved as a promise
     *
     * @param builder The message builder to transform
     * @param temporaryDataMapper the mapping function of the values
     * @param permanentDataMapper the mapping function of the values
     * @param maskedDataMapper the mapping function of the values that need to be masked
     * @return Updated builder with the mapper applied to all sensitive fields
     */
    public static <T extends Builder> T mapSensitiveFields(final T builder,
                                                           final SensitiveDataFunction temporaryDataMapper,
                                                           final SensitiveDataFunction permanentDataMapper,
                                                           final SensitiveDataFunction maskedDataMapper) {
        final SensitiveDataCollection sensitiveDataCollection = new SensitiveDataCollection();
        collectSensitiveFields(sensitiveDataCollection, builder, new HashSet<>());
        
        final List<SensitiveDataContext> mappedTemporaryData = temporaryDataMapper.apply(sensitiveDataCollection.getTemporaryData());
        final List<SensitiveDataContext> mappedPermanentData = permanentDataMapper.apply(sensitiveDataCollection.getPermanentData());
        final List<SensitiveDataContext> mappedMaskedData = maskedDataMapper.apply(sensitiveDataCollection.getMaskedData());
        
        return applyMappers(builder, sensitiveDataCollection, mappedTemporaryData, mappedPermanentData, mappedMaskedData);
    }
    
    public static <T extends Builder> T applyMappers(final T builder,
                                                     final SensitiveDataCollection sensitiveDataCollection,
                                                     final List<SensitiveDataContext> mappedTemporaryData,
                                                     final List<SensitiveDataContext> mappedPermanentData,
                                                     final List<SensitiveDataContext> mappedMaskedData) {
        if (mappedTemporaryData.size() != sensitiveDataCollection.getTemporaryData().size()) {
            throw new IllegalStateException("Size of temporary sensitive data list altered during mapping!");
        }
        if (mappedPermanentData.size() != sensitiveDataCollection.getPermanentData().size()) {
            throw new IllegalStateException("Size of permanent sensitive data list altered during mapping!");
        }
        if (mappedMaskedData.size() != sensitiveDataCollection.getMaskedData().size()) {
            throw new IllegalStateException("Size of masked sensitive data list altered during mapping!");
        }
        
        final SensitiveDataCollection updatedSensitiveData = new SensitiveDataCollection(mappedTemporaryData, mappedPermanentData, mappedMaskedData);
        mapSensitiveFields(builder, (o, sensitiveDataLifetime) -> {
            if (sensitiveDataLifetime == SensitiveDataLifetime.TEMPORARY) {
                return updatedSensitiveData.popTemporaryData();
            } else if (sensitiveDataLifetime == SensitiveDataLifetime.PERMANENT) {
                return updatedSensitiveData.popPermanentData();
            } else if (sensitiveDataLifetime == SensitiveDataLifetime.MASKED) {
                return updatedSensitiveData.popMaskedData();
            } else {
                return new SensitiveDataContext("", o);
            }
        });
        return builder;
    }
    
    public static boolean collectSensitiveFields(final SensitiveDataCollection sensitiveDataCollection,
                                                 final MessageOrBuilder messageOrBuilder,
                                                 final Set<Class<?>> typesBeingScanned) {
        final Class<? extends Message> messageType = messageOrBuilder.getDefaultInstanceForType().getClass();
        if (BUILDERS_WITH_NO_SENSITIVE_FIELDS.contains(messageType)) {
            return false;
        }
        final boolean beingScanned = !typesBeingScanned.add(messageType);
        
        boolean hasSensitiveFields = false;
        for (final FieldDescriptor fieldDescriptor : messageOrBuilder.getDescriptorForType().getFields()) {
            
            if (fieldDescriptor.isRepeated()) {
                if (isRepeatedMessage(fieldDescriptor)) {
                    hasSensitiveFields |= collectSensitiveRepeatedField(sensitiveDataCollection, typesBeingScanned, messageOrBuilder, messageType, fieldDescriptor);
                }
            } else if (fieldDescriptor.getJavaType() == JavaType.MESSAGE) {
                final Message field = (Message) messageOrBuilder.getField(fieldDescriptor);
                hasSensitiveFields |= collectSensitiveMessageFields(sensitiveDataCollection, typesBeingScanned, field);
            } else {
                final Optional<SensitiveDataLifetime> maybeSensitiveDataLifetime = resolveSensitiveData(fieldDescriptor);
                if (maybeSensitiveDataLifetime.isPresent()) {
                    hasSensitiveFields = true;
                    final SensitiveDataLifetime sensitiveDataLifetime = maybeSensitiveDataLifetime.get();
                    Object value = messageOrBuilder.getField(fieldDescriptor);
                    if (!value.toString().equals("")) {
                        sensitiveDataCollection.addData(sensitiveDataLifetime, fieldDescriptor.getJsonName(), value);
                    }
                }
            }
        }
        
        if (!hasSensitiveFields) {
            BUILDERS_WITH_NO_SENSITIVE_FIELDS.add(messageType);
        }
        if (!beingScanned) {
            typesBeingScanned.remove(messageType);
        }
        return hasSensitiveFields;
    }
    
    private static boolean isRepeatedMessage(final FieldDescriptor fieldDescriptor) {
        if (fieldDescriptor.getJavaType() == JavaType.MESSAGE) {
            if (fieldDescriptor.isMapField()) {
                final FieldDescriptor valueDescriptor = fieldDescriptor.getMessageType().getFields().get(1);
                return valueDescriptor.getJavaType() == JavaType.MESSAGE;
            } else {
                return true;
            }
        }
        return false;
    }
    
    private static boolean collectSensitiveMessageFields(final SensitiveDataCollection sensitiveDataCollection, final Set<Class<?>> typesBeingScanned, final Message field) {
        if ((field != field.getDefaultInstanceForType()) || !typesBeingScanned.contains(field.getDefaultInstanceForType().getClass())) {
            return collectSensitiveFields(sensitiveDataCollection, field, typesBeingScanned);
        }
        return false;
    }
    
    private static boolean collectSensitiveRepeatedField(final SensitiveDataCollection sensitiveDataCollection,
                                                         final Set<Class<?>> typesBeingScanned,
                                                         final MessageOrBuilder messageOrBuilder,
                                                         final Class<? extends Message> messageType,
                                                         final FieldDescriptor fieldDescriptor) {
        boolean hasSensitiveFields = false;
        final int repeatedFieldCount = messageOrBuilder.getRepeatedFieldCount(fieldDescriptor);
        for (int i = 0; i < repeatedFieldCount; i++) {
            if (BUILDERS_WITH_NO_SENSITIVE_FIELDS.contains(messageType)) {
                break;
            }
            Message repeated;
            if (fieldDescriptor.isMapField()) {
                final MapEntry<?, ?> mapEntry = (MapEntry<?, ?>) messageOrBuilder.getRepeatedField(fieldDescriptor, i);
                repeated = (Message) mapEntry.getValue();
            } else {
                repeated = (Message) messageOrBuilder.getRepeatedField(fieldDescriptor, i);
            }
            hasSensitiveFields |= collectSensitiveMessageFields(sensitiveDataCollection, typesBeingScanned, repeated);
            // repeated fields should not be marked sensitive themselves
        }
        return hasSensitiveFields;
    }
    
    /**
     * Apply a mapper function on all sensitive fields of the message (including repeated and nested)
     *
     * @param builder The message builder to transform
     * @param mapper the mapping function of the values
     * @return Updated builder with the mapper applied to all sensitive fields
     */
    private static Builder mapSensitiveFields(final Builder builder, final BiFunction<Object, SensitiveDataLifetime, SensitiveDataContext> mapper) {
        final Class<? extends Message> messageType = builder.getDefaultInstanceForType().getClass();
        if (BUILDERS_WITH_NO_SENSITIVE_FIELDS.contains(messageType)) {
            return builder;
        }
        
        final Map<FieldDescriptor, Object> allFields = builder.getAllFields();
        for (final Entry<FieldDescriptor, Object> descriptorObjectEntry : allFields.entrySet()) {
            final FieldDescriptor fieldDescriptor = descriptorObjectEntry.getKey();
            
            if (fieldDescriptor.isRepeated()) {
                boolean repeatedMessage = false;
                FieldDescriptor valueDescriptor = null;
                if (fieldDescriptor.getJavaType() == JavaType.MESSAGE) {
                    if (fieldDescriptor.isMapField()) {
                        valueDescriptor = fieldDescriptor.getMessageType().getFields().get(1);
                        if (valueDescriptor.getJavaType() == JavaType.MESSAGE) {
                            repeatedMessage = true;
                        }
                    } else {
                        repeatedMessage = true;
                    }
                }
                
                if (repeatedMessage) {
                    final int repeatedFieldCount = builder.getRepeatedFieldCount(fieldDescriptor);
                    for (int i = 0; i < repeatedFieldCount; i++) {
                        if (BUILDERS_WITH_NO_SENSITIVE_FIELDS.contains(messageType)) {
                            break;
                        }
                        if (fieldDescriptor.isMapField()) {
                            final MapEntry<?, ?> mapEntry = (MapEntry<?, ?>) builder.getRepeatedField(fieldDescriptor, i);
                            final Message repeated = (Message) mapEntry.getValue();
                            if ((repeated != null) && (repeated != repeated.getDefaultInstanceForType())) {
                                builder.setRepeatedField(fieldDescriptor,
                                    i,
                                    mapEntry.toBuilder()
                                        .setField(valueDescriptor, mapSensitiveFields(repeated.toBuilder(), mapper).build())
                                        .build());
                            }
                        } else {
                            final Message repeated = (Message) builder.getRepeatedField(fieldDescriptor, i);
                            if ((repeated != null) && (repeated != repeated.getDefaultInstanceForType())) {
                                builder.setRepeatedField(fieldDescriptor, i, mapSensitiveFields(repeated.toBuilder(), mapper).build());
                            }
                        }
                    }
                    // repeated fields should not be marked sensitive themselves
                }
            } else if (fieldDescriptor.getJavaType() == JavaType.MESSAGE) {
                final Message field = (Message) builder.getField(fieldDescriptor);
                if (field != field.getDefaultInstanceForType()) {
                    final Builder mappedNestedField = mapSensitiveFields(field.toBuilder(), mapper);
                    builder.setField(fieldDescriptor, mappedNestedField.build());
                }
            } else if (!descriptorObjectEntry.getValue().toString().equals("")) {
                final Optional<SensitiveDataLifetime> maybeSensitiveDataLifetime = resolveSensitiveData(fieldDescriptor);
                if (maybeSensitiveDataLifetime.isPresent()) {
                    final SensitiveDataContext updatedValue = mapper.apply(descriptorObjectEntry.getValue(), maybeSensitiveDataLifetime.get());
                    builder.setField(fieldDescriptor, updatedValue.getSensitiveData());
                }
            }
        }
        return builder;
    }
    
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public static class SensitiveDataCollection {
        
        private final List<SensitiveDataContext> temporaryData;
        private final List<SensitiveDataContext> permanentData;
        private final List<SensitiveDataContext> maskedData;
        
        public SensitiveDataCollection() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        
        SensitiveDataCollection(final List<SensitiveDataContext> temporaryData,
                                final List<SensitiveDataContext> permanentData,
                                final List<SensitiveDataContext> maskedData) {
            this.temporaryData = temporaryData;
            this.permanentData = permanentData;
            this.maskedData = maskedData;
        }
        
        void addData(final SensitiveDataLifetime sensitiveDataLifetime, final String fieldName, final Object sensitiveData) {
            if (sensitiveDataLifetime == SensitiveDataLifetime.TEMPORARY) {
                temporaryData.add(new SensitiveDataContext(fieldName, sensitiveData));
            } else if (sensitiveDataLifetime == SensitiveDataLifetime.PERMANENT) {
                permanentData.add(new SensitiveDataContext(fieldName, sensitiveData));
            } else if (sensitiveDataLifetime == SensitiveDataLifetime.MASKED) {
                maskedData.add(new SensitiveDataContext(fieldName, sensitiveData));
            }
        }
        
        public List<SensitiveDataContext> getTemporaryData() {
            return temporaryData;
        }
        
        SensitiveDataContext popTemporaryData() {
            return temporaryData.remove(0);
        }
        
        public List<SensitiveDataContext> getPermanentData() {
            return permanentData;
        }
        
        SensitiveDataContext popPermanentData() {
            return permanentData.remove(0);
        }
        
        public List<SensitiveDataContext> getMaskedData() {
            return maskedData;
        }
        
        SensitiveDataContext popMaskedData() {
            return maskedData.remove(0);
        }
        
    }
    
    public static class SensitiveDataContext {
        
        private final String fieldName;
        private final Object sensitiveData;
        
        public SensitiveDataContext(final String fieldName, final Object sensitiveData) {
            this.fieldName = fieldName;
            this.sensitiveData = sensitiveData;
        }
        
        public String getFieldName() {
            return fieldName;
        }
        
        public Object getSensitiveData() {
            return sensitiveData;
        }
        
        public SensitiveDataContext transformSensitiveData(final Object transformedData) {
            return new SensitiveDataContext(fieldName, transformedData);
        }
        
    }
    
}
