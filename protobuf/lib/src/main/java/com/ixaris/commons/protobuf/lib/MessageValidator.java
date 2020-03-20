package com.ixaris.commons.protobuf.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.google.protobuf.ProtocolMessageEnum;

import com.ixaris.commons.misc.lib.conversion.SnakeCaseHelper;
import com.ixaris.commons.misc.lib.lock.LockUtil;

import valid.Valid.FieldValidation;
import valid.Valid.FieldValidationErrors;
import valid.Valid.MessageValidation;

/**
 * Validator for protobuf messages
 *
 * <p>Handles primitive types boolean, int, long, float, double as well as String and Objects. Also handles lists of all these types with support
 * for validating
 *
 * @author brian.vella
 */
public final class MessageValidator {
    
    private static final Map<Class<? extends MessageLite>, BiConsumer<? extends MessageLite, RootValidator>> VALIDATION_CONSUMERS_MAP = new HashMap<>();
    private static final BiConsumer<?, RootValidator> NO_VALIDATION = (m, v) -> {};
    
    // this pattern marches any string starting from the beginning or with . and ending with opening square bracket
    // this effectively matches field names without map key
    private static final Pattern FIELD_NAME_OMITTING_MAP_KEY_PATTERN = Pattern.compile("((?:^|\\.)[^\\[]+)");
    
    /**
     * To be used when reporting validation errors to schemas generated from protobuf and converting snake_case to camelCase, e.g. json. This
     * preservices the map key in between square brackets, as that is user supplied, e.g. some_field[MaPkEy]
     *
     * @param field
     * @return
     */
    public static String fieldSnakeToCamelCasePreservingMapKey(final String field) {
        if (field.isEmpty()) {
            return field;
        }
        final Matcher m = FIELD_NAME_OMITTING_MAP_KEY_PATTERN.matcher(field);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, SnakeCaseHelper.snakeToCamelCase(m.group(1)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    public static <T extends MessageLite> MessageValidation validate(final T message) {
        final BiConsumer<T, RootValidator> consumer = resolveValidationConsumer(message);
        final RootValidator validator = new RootValidator();
        consumer.accept(message, validator);
        
        final MessageValidation.Builder messageValidationBuilder = MessageValidation.newBuilder();
        if (!validator.fieldValidations.isEmpty()) {
            messageValidationBuilder.setInvalid(true);
            validator.fieldValidations.forEach((key, value) -> {
                final FieldValidationErrors.Builder fieldValidationErrorsBuilder = FieldValidationErrors.newBuilder().setName(key);
                value.forEach((type, params) -> fieldValidationErrorsBuilder.addErrors(FieldValidation.newBuilder().setType(type).addAllParams(params)));
                messageValidationBuilder.addFields(fieldValidationErrorsBuilder.build());
            });
        }
        return messageValidationBuilder.build();
    }
    
    public static MessageValidation getValidationMessage(final String name, final FieldValidation.Type type, final String... params) {
        return MessageValidation.newBuilder()
            .setInvalid(true)
            .addFields(FieldValidationErrors.newBuilder()
                .setName(name)
                .addErrors(FieldValidation.newBuilder().setType(type).addAllParams(Arrays.asList(params))))
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends MessageLite> BiConsumer<T, RootValidator> resolveValidationConsumer(final T message) {
        final Class<? extends MessageLite> type = message.getClass();
        BiConsumer<T, RootValidator> validatorConsumer = (BiConsumer<T, RootValidator>) VALIDATION_CONSUMERS_MAP.get(type);
        if (validatorConsumer == null) {
            try {
                final Class<?> validationType = Class.forName(type.getName().replace("$", "Validation$") + "Validation");
                validatorConsumer = (BiConsumer<T, RootValidator>) validationType.getMethod("getValidator").invoke(null);
            } catch (final ClassNotFoundException e) {
                validatorConsumer = (BiConsumer<T, RootValidator>) NO_VALIDATION;
            } catch (final IllegalArgumentException | ReflectiveOperationException | SecurityException e) {
                throw new IllegalStateException(e);
            }
            VALIDATION_CONSUMERS_MAP.put(type, validatorConsumer);
        }
        return validatorConsumer;
    }
    
    @FunctionalInterface
    public interface ListItemCallback<V> {
        
        void onItem(V v);
        
    }
    
    @FunctionalInterface
    public interface MapEntryCallback<K, V> {
        
        void onEntry(K k, V v);
        
    }
    
    public static final class RootValidator {
        
        private final String prefix;
        private final Map<String, Map<FieldValidation.Type, Iterable<String>>> fieldValidations;
        private final Map<String, FieldValidator<?>> fieldValidators;
        
        RootValidator() {
            this.prefix = "";
            this.fieldValidations = new LinkedHashMap<>();
            this.fieldValidators = new HashMap<>();
        }
        
        RootValidator(final String prefix, final RootValidator rootValidator) {
            this.prefix = prefix;
            this.fieldValidations = rootValidator.fieldValidations;
            this.fieldValidators = rootValidator.fieldValidators;
        }
        
        public BooleanFieldValidator field(final String name, final boolean value) {
            return addValidator(name, new BooleanFieldValidator(this, name, value, value));
        }
        
        public BooleanFieldValidator field(final String name, final Boolean value) {
            return addValidator(name, new BooleanFieldValidator(this, name, value != null, value != null ? value : false));
        }
        
        public BooleanListValidator listBoolean(final String name, final List<Boolean> list) {
            return addValidator(name, new BooleanListValidator(this, name, list));
        }
        
        public BooleanMapNumberKeyValidator<Integer> mapIntBoolean(final String name, final Map<Integer, Boolean> map) {
            return addValidator(name, new BooleanMapNumberKeyValidator<>(this, name, map));
        }
        
        public BooleanMapNumberKeyValidator<Long> mapLongBoolean(final String name, final Map<Long, Boolean> map) {
            return addValidator(name, new BooleanMapNumberKeyValidator<>(this, name, map));
        }
        
        public BooleanMapStringKeyValidator mapStringBoolean(final String name, final Map<String, Boolean> map) {
            return addValidator(name, new BooleanMapStringKeyValidator(this, name, map));
        }
        
        public <E extends Enum<E> & ProtocolMessageEnum> BooleanMapEnumKeyValidator<E> mapEnumBoolean(final String name,
                                                                                                      final Class<E> type,
                                                                                                      final Map<String, Boolean> map) {
            return addValidator(name, new BooleanMapEnumKeyValidator<>(this, name, type, map));
        }
        
        public NumberFieldValidator<Integer> field(final String name, final int value) {
            return addValidator(name, new NumberFieldValidator<>(this, name, value != 0, value));
        }
        
        public NumberListValidator<Integer> listInt(final String name, final List<Integer> list) {
            return addValidator(name, new NumberListValidator<>(this, name, list));
        }
        
        public NumberMapNumberKeyValidator<Integer, Integer> mapIntInt(final String name, final Map<Integer, Integer> map) {
            return addValidator(name, new NumberMapNumberKeyValidator<>(this, name, map));
        }
        
        public NumberMapNumberKeyValidator<Long, Integer> mapLongInt(final String name, final Map<Long, Integer> map) {
            return addValidator(name, new NumberMapNumberKeyValidator<>(this, name, map));
        }
        
        public NumberMapStringKeyValidator<Integer> mapStringInt(final String name, final Map<String, Integer> map) {
            return addValidator(name, new NumberMapStringKeyValidator<>(this, name, map));
        }
        
        public <E extends Enum<E> & ProtocolMessageEnum> NumberMapEnumKeyValidator<Integer, E> mapEnumInt(final String name,
                                                                                                          final Class<E> type,
                                                                                                          final Map<String, Integer> map) {
            return addValidator(name, new NumberMapEnumKeyValidator<>(this, name, type, map));
        }
        
        public NumberFieldValidator<Long> field(final String name, final long value) {
            return addValidator(name, new NumberFieldValidator<>(this, name, value != 0L, value));
        }
        
        public NumberListValidator<Long> listLong(final String name, final List<Long> list) {
            return addValidator(name, new NumberListValidator<>(this, name, list));
        }
        
        public NumberMapNumberKeyValidator<Integer, Long> mapIntLong(final String name, final Map<Integer, Long> map) {
            return addValidator(name, new NumberMapNumberKeyValidator<>(this, name, map));
        }
        
        public NumberMapNumberKeyValidator<Long, Long> mapLongLong(final String name, final Map<Long, Long> map) {
            return addValidator(name, new NumberMapNumberKeyValidator<>(this, name, map));
        }
        
        public NumberMapStringKeyValidator<Long> mapStringLong(final String name, final Map<String, Long> map) {
            return addValidator(name, new NumberMapStringKeyValidator<>(this, name, map));
        }
        
        public <E extends Enum<E> & ProtocolMessageEnum> NumberMapEnumKeyValidator<Long, E> mapEnumLong(final String name,
                                                                                                        final Class<E> type,
                                                                                                        final Map<String, Long> map) {
            return addValidator(name, new NumberMapEnumKeyValidator<>(this, name, type, map));
        }
        
        public NumberFieldValidator<Float> field(final String name, final float value) {
            return addValidator(name, new NumberFieldValidator<>(this, name, Float.compare(value, 0.0f) != 0, value));
        }
        
        public NumberListValidator<Float> listFloat(final String name, final List<Float> list) {
            return addValidator(name, new NumberListValidator<>(this, name, list));
        }
        
        public NumberMapNumberKeyValidator<Integer, Float> mapIntFloat(final String name, final Map<Integer, Float> map) {
            return addValidator(name, new NumberMapNumberKeyValidator<>(this, name, map));
        }
        
        public NumberMapNumberKeyValidator<Long, Float> mapLongFloat(final String name, final Map<Long, Float> map) {
            return addValidator(name, new NumberMapNumberKeyValidator<>(this, name, map));
        }
        
        public NumberMapStringKeyValidator<Float> mapStringFloat(final String name, final Map<String, Float> map) {
            return addValidator(name, new NumberMapStringKeyValidator<>(this, name, map));
        }
        
        public <E extends Enum<E> & ProtocolMessageEnum> NumberMapEnumKeyValidator<Float, E> mapEnumFloat(final String name,
                                                                                                          final Class<E> type,
                                                                                                          final Map<String, Float> map) {
            return addValidator(name, new NumberMapEnumKeyValidator<>(this, name, type, map));
        }
        
        public NumberFieldValidator<Double> field(final String name, final double value) {
            return addValidator(name, new NumberFieldValidator<>(this, name, Double.compare(value, 0.0) != 0, value));
        }
        
        public NumberListValidator<Double> listDouble(final String name, final List<Double> list) {
            return addValidator(name, new NumberListValidator<>(this, name, list));
        }
        
        public NumberMapNumberKeyValidator<Integer, Double> mapIntDouble(final String name, final Map<Integer, Double> map) {
            return addValidator(name, new NumberMapNumberKeyValidator<>(this, name, map));
        }
        
        public NumberMapNumberKeyValidator<Long, Double> mapLongDouble(final String name, final Map<Long, Double> map) {
            return addValidator(name, new NumberMapNumberKeyValidator<>(this, name, map));
        }
        
        public NumberMapStringKeyValidator<Double> mapStringDouble(final String name, final Map<String, Double> map) {
            return addValidator(name, new NumberMapStringKeyValidator<>(this, name, map));
        }
        
        public <E extends Enum<E> & ProtocolMessageEnum> NumberMapEnumKeyValidator<Double, E> mapEnumDouble(final String name,
                                                                                                            final Class<E> type,
                                                                                                            final Map<String, Double> map) {
            return addValidator(name, new NumberMapEnumKeyValidator<>(this, name, type, map));
        }
        
        public StringFieldValidator field(final String name, final String value) {
            return addValidator(name, new StringFieldValidator(this, name, value));
        }
        
        public StringListValidator listString(final String name, final List<String> list) {
            return addValidator(name, new StringListValidator(this, name, list));
        }
        
        public StringMapNumberKeyValidator<Integer> mapIntString(final String name, final Map<Integer, String> map) {
            return addValidator(name, new StringMapNumberKeyValidator<>(this, name, map));
        }
        
        public StringMapNumberKeyValidator<Long> mapLongString(final String name, final Map<Long, String> map) {
            return addValidator(name, new StringMapNumberKeyValidator<>(this, name, map));
        }
        
        public StringMapStringKeyValidator mapStringString(final String name, final Map<String, String> map) {
            return addValidator(name, new StringMapStringKeyValidator(this, name, map));
        }
        
        public <E extends Enum<E> & ProtocolMessageEnum> StringMapEnumKeyValidator<E> mapEnumString(final String name,
                                                                                                    final Class<E> type,
                                                                                                    final Map<String, String> map) {
            return addValidator(name, new StringMapEnumKeyValidator<>(this, name, type, map));
        }
        
        public BytesFieldValidator field(final String name, final ByteString value) {
            return addValidator(name, new BytesFieldValidator(this, name, value));
        }
        
        public BytesListValidator listBytes(final String name, final List<ByteString> list) {
            return addValidator(name, new BytesListValidator(this, name, list));
        }
        
        public BytesMapNumberKeyValidator<Integer> mapIntBytes(final String name, final Map<Integer, ByteString> map) {
            return addValidator(name, new BytesMapNumberKeyValidator<>(this, name, map));
        }
        
        public BytesMapNumberKeyValidator<Long> mapLongBytes(final String name, final Map<Long, ByteString> map) {
            return addValidator(name, new BytesMapNumberKeyValidator<>(this, name, map));
        }
        
        public BytesMapStringKeyValidator mapStringBytes(final String name, final Map<String, ByteString> map) {
            return addValidator(name, new BytesMapStringKeyValidator(this, name, map));
        }
        
        public <E extends Enum<E> & ProtocolMessageEnum> BytesMapEnumKeyValidator<E> mapEnumBytes(final String name,
                                                                                                  final Class<E> type,
                                                                                                  final Map<String, ByteString> map) {
            return addValidator(name, new BytesMapEnumKeyValidator<>(this, name, type, map));
        }
        
        public <T extends MessageLite> MessageFieldValidator<T> field(final String name, final T value) {
            return addValidator(name, new MessageFieldValidator<>(this, name, value));
        }
        
        public <T extends MessageLite> MessageListValidator<T> listMessage(final String name, final List<T> list) {
            return addValidator(name, new MessageListValidator<>(this, name, list));
        }
        
        public <T extends MessageLite> MessageMapNumberKeyValidator<Integer, T> mapIntMessage(final String name, final Map<Integer, T> map) {
            return addValidator(name, new MessageMapNumberKeyValidator<>(this, name, map));
        }
        
        public <T extends MessageLite> MessageMapNumberKeyValidator<Long, T> mapLongMessage(final String name, final Map<Long, T> map) {
            return addValidator(name, new MessageMapNumberKeyValidator<>(this, name, map));
        }
        
        public <T extends MessageLite> MessageMapStringKeyValidator<T> mapStringMessage(final String name, final Map<String, T> map) {
            return addValidator(name, new MessageMapStringKeyValidator<>(this, name, map));
        }
        
        public <T extends MessageLite, E extends Enum<E> & ProtocolMessageEnum> MessageMapEnumKeyValidator<T, E> mapEnumMessage(final String name,
                                                                                                                                final Class<E> type,
                                                                                                                                final Map<String, T> map) {
            return addValidator(name, new MessageMapEnumKeyValidator<>(this, name, type, map));
        }
        
        public <T extends Enum<T> & ProtocolMessageEnum> EnumFieldValidator<T> field(final String name, final T value) {
            return addValidator(name, new EnumFieldValidator<>(this, name, value));
        }
        
        public <T extends Enum<T> & ProtocolMessageEnum> EnumListValidator<T> listEnum(final String name, final List<T> list) {
            return addValidator(name, new EnumListValidator<>(this, name, list));
        }
        
        public <T extends Enum<T> & ProtocolMessageEnum> EnumMapNumberKeyValidator<Integer, T> mapIntEnum(final String name,
                                                                                                          final Map<Integer, T> map) {
            return addValidator(name, new EnumMapNumberKeyValidator<>(this, name, map));
        }
        
        public <T extends Enum<T> & ProtocolMessageEnum> EnumMapNumberKeyValidator<Long, T> maplongEnum(final String name, final Map<Long, T> map) {
            return addValidator(name, new EnumMapNumberKeyValidator<>(this, name, map));
        }
        
        public <T extends Enum<T> & ProtocolMessageEnum> EnumMapStringKeyValidator<T> mapStringEnum(final String name, final Map<String, T> map) {
            return addValidator(name, new EnumMapStringKeyValidator<>(this, name, map));
        }
        
        /**
         * Ensure that there are exactly n present fields from the given fields.
         */
        public RootValidator exactlyN(final int n, final String... fields) {
            if ((n > 0) && (fields != null)) {
                final long np = Arrays.stream(fields)
                    .map(field -> Optional.ofNullable(fieldValidators.get(prefix + field)).map(v -> v.present).orElse(false))
                    .filter(x -> x)
                    .count();
                if (np != n) {
                    invalid(prefix, FieldValidation.Type.EXACTLY, join(n, fields));
                }
            }
            
            return this;
        }
        
        /**
         * Ensure that at least N of the given fields are present
         */
        public RootValidator atLeastN(final int n, final String... fields) {
            
            if ((n > 0) && (fields != null)) {
                final long np = Arrays.stream(fields)
                    .map(field -> Optional.ofNullable(fieldValidators.get(prefix + field)).map(v -> v.present).orElse(false))
                    .filter(x -> x)
                    .count();
                if (np < n) {
                    invalid(prefix, FieldValidation.Type.AT_LEAST, join(n, fields));
                }
            }
            
            return this;
        }
        
        /**
         * Ensure that at least N of the given fields are present
         */
        public RootValidator atMostN(final int n, final String... fields) {
            if ((n > 0) && (fields != null)) {
                final long np = Arrays.stream(fields)
                    .map(field -> Optional.ofNullable(fieldValidators.get(prefix + field)).map(v -> v.present).orElse(false))
                    .filter(x -> x)
                    .count();
                if (np > n) {
                    invalid(prefix, FieldValidation.Type.AT_MOST, join(n, fields));
                }
            }
            
            return this;
        }
        
        /**
         * Ensure that the fields of the given values match. Matching is done via {@link Object#equals(Object)}.
         */
        public RootValidator allOrNone(final String... fields) {
            if (fields.length > 0) {
                final boolean present = Optional.ofNullable(fieldValidators.get(prefix + fields[0])).map(v -> v.present).orElse(false);
                if (Arrays.stream(fields)
                    .skip(1)
                    .anyMatch(f -> present != Optional.ofNullable(fieldValidators.get(prefix + f)).map(v -> v.present).orElse(false))) {
                    
                    invalid(prefix, FieldValidation.Type.ALL_OR_NONE, fields);
                }
            }
            
            return this;
        }
        
        private <N extends Number> NumberFieldValidator<N> field(final String name, final N value) {
            return addValidator(name, new NumberFieldValidator<>(this, name, !NumberHelper.isDefault(value), value));
        }
        
        private <N extends Number> NumberListValidator<N> listNumber(final String name, final List<N> list) {
            return addValidator(name, new NumberListValidator<>(this, name, list));
        }
        
        private <K extends Number, N extends Number> NumberMapNumberKeyValidator<K, N> mapNumberNumber(final String name, final Map<K, N> list) {
            return addValidator(name, new NumberMapNumberKeyValidator<>(this, name, list));
        }
        
        private <N extends Number> NumberMapStringKeyValidator<N> mapStringNumber(final String name, final Map<String, N> list) {
            return addValidator(name, new NumberMapStringKeyValidator<>(this, name, list));
        }
        
        private <N extends Number, E extends Enum<E> & ProtocolMessageEnum> NumberMapEnumKeyValidator<N, E> mapStringNumber(final String name,
                                                                                                                            final Class<E> type,
                                                                                                                            final Map<String, N> list) {
            return addValidator(name, new NumberMapEnumKeyValidator<>(this, name, type, list));
        }
        
        private void invalid(final String name, final FieldValidation.Type type, final String... params) {
            invalid(name, type, Arrays.asList(params));
        }
        
        private void invalid(final String name, final FieldValidation.Type type, final Iterable<String> params) {
            final String key = prefix + name;
            final Map<FieldValidation.Type, Iterable<String>> map = fieldValidations.computeIfAbsent(key, k -> new HashMap<>());
            map.put(type, params != null ? params : Collections.emptyList());
        }
        
        private <T extends FieldValidator<T>> T addValidator(final String name, final T validator) {
            fieldValidators.put(prefix + name, validator);
            return validator;
        }
        
    }
    
    private abstract static class FieldValidator<T extends FieldValidator<T>> {
        
        protected final RootValidator validator;
        protected final String name;
        protected final boolean present;
        
        FieldValidator(final RootValidator validator, final String name, final boolean present) {
            this.validator = validator;
            this.name = name;
            this.present = present;
        }
        
        @SuppressWarnings("unchecked")
        public final T required() {
            if (!present) {
                validator.invalid(name, FieldValidation.Type.REQUIRED);
            }
            return (T) this;
        }
        
        @SuppressWarnings("unchecked")
        public final T requires(final String... names) {
            if (present
                && !Arrays.stream(names).allMatch(n -> Optional.ofNullable(validator.fieldValidators.get(validator.prefix + n)).map(v -> v.present).orElse(false))) {
                validator.invalid(name, FieldValidation.Type.REQUIRES, names);
            }
            return (T) this;
        }
        
        public abstract Object getValue();
        
    }
    
    private abstract static class ListValidator<V, T extends ListValidator<V, T>> extends FieldValidator<T> {
        
        protected final List<V> list;
        
        public ListValidator(final RootValidator validator, final String name, final List<V> list) {
            super(validator, name, !list.isEmpty());
            this.list = list;
        }
        
        @Override
        public List<V> getValue() {
            return list;
        }
        
        @SuppressWarnings("unchecked")
        public T size(final Integer min, final Integer max) {
            if (present) {
                boolean invalid = false;
                if ((min != null) && (list.size() < min)) {
                    invalid = true;
                }
                if (!invalid && (max != null) && (list.size() > max)) {
                    invalid = true;
                }
                if (invalid) {
                    validator.invalid(name, FieldValidation.Type.SIZE, min != null ? min.toString() : "", max != null ? max.toString() : "");
                }
            }
            return (T) this;
        }
        
    }
    
    private abstract static class MapValidator<K, V, T extends MapValidator<K, V, T>> extends FieldValidator<T> {
        
        protected final Map<K, V> map;
        
        public MapValidator(final RootValidator validator, final String name, final Map<K, V> map) {
            super(validator, name, !map.isEmpty());
            this.map = map;
        }
        
        @Override
        public Map<K, V> getValue() {
            return map;
        }
        
        @SuppressWarnings("unchecked")
        public T size(final Integer min, final Integer max) {
            if (present) {
                boolean invalid = false;
                if ((min != null) && (map.size() < min)) {
                    invalid = true;
                }
                if (!invalid && (max != null) && (map.size() > max)) {
                    invalid = true;
                }
                if (invalid) {
                    validator.invalid(name, FieldValidation.Type.SIZE, min != null ? min.toString() : "", max != null ? max.toString() : "");
                }
            }
            return (T) this;
        }
    }
    
    private abstract static class MapNumberKeyValidator<K extends Number, V, T extends MapNumberKeyValidator<K, V, T>> extends MapValidator<K, V, T> {
        
        public MapNumberKeyValidator(final RootValidator validator, final String name, final Map<K, V> map) {
            super(validator, name, map);
        }
        
    }
    
    private abstract static class MapStringKeyValidator<V, T extends MapStringKeyValidator<V, T>> extends MapValidator<String, V, T> {
        
        public MapStringKeyValidator(final RootValidator validator, final String name, final Map<String, V> map) {
            super(validator, name, map);
        }
        
    }
    
    public static final class NumberFieldValidator<V extends Number> extends FieldValidator<NumberFieldValidator<V>> {
        
        private final V value;
        
        private NumberFieldValidator(final RootValidator validator, final String name, final boolean present, final V value) {
            super(validator, name, present);
            this.value = value;
        }
        
        public NumberFieldValidator<V> range(final Number min, final Number max, final boolean exclusive) {
            if (present) {
                boolean invalid = false;
                if (min != null) {
                    invalid = isInvalidMin(min, exclusive);
                }
                if (!invalid && (max != null)) {
                    invalid = isInvalidMax(max, exclusive);
                }
                if (invalid) {
                    validator.invalid(name,
                        FieldValidation.Type.RANGE,
                        min != null ? min.toString() : "",
                        max != null ? max.toString() : "",
                        Boolean.toString(exclusive));
                }
            }
            return this;
        }
        
        public NumberFieldValidator<V> range(final String min, final Number max, final boolean exclusive) {
            if (present) {
                boolean invalid = false;
                if (min != null) {
                    invalid = isInvalidMin(min, exclusive);
                }
                if (!invalid && (max != null)) {
                    invalid = isInvalidMax(max, exclusive);
                }
                if (invalid) {
                    validator.invalid(name,
                        FieldValidation.Type.RANGE,
                        min != null ? min : "",
                        max != null ? max.toString() : "",
                        Boolean.toString(exclusive));
                }
            }
            return this;
        }
        
        public NumberFieldValidator<V> range(final Number min, final String max, final boolean exclusive) {
            if (present) {
                boolean invalid = false;
                if (min != null) {
                    invalid = isInvalidMin(min, exclusive);
                }
                if (!invalid && (max != null)) {
                    invalid = isInvalidMax(max, exclusive);
                }
                if (invalid) {
                    validator.invalid(name,
                        FieldValidation.Type.RANGE,
                        min != null ? min.toString() : "",
                        max != null ? max : "",
                        Boolean.toString(exclusive));
                }
            }
            return this;
        }
        
        public NumberFieldValidator<V> range(final String min, final String max, final boolean exclusive) {
            if (present) {
                boolean invalid = false;
                if (min != null) {
                    invalid = isInvalidMin(min, exclusive);
                }
                if (!invalid && (max != null)) {
                    invalid = isInvalidMax(max, exclusive);
                }
                if (invalid) {
                    validator.invalid(name,
                        FieldValidation.Type.RANGE,
                        min != null ? min : "",
                        max != null ? max : "",
                        Boolean.toString(exclusive));
                }
            }
            return this;
        }
        
        private boolean isInvalidMin(final Number min, final boolean exclusive) {
            final int compare = NumberHelper.compare(value, min);
            return exclusive ? (compare <= 0) : (compare < 0);
        }
        
        private boolean isInvalidMin(final String minField, final boolean exclusive) {
            final NumberFieldValidator<?> v = (NumberFieldValidator<?>) validator.fieldValidators.get(validator.prefix + minField);
            final int compare = NumberHelper.compare(value, v.value);
            return exclusive ? (compare <= 0) : (compare < 0);
        }
        
        private boolean isInvalidMax(final Number max, final boolean exclusive) {
            final int compare = NumberHelper.compare(value, max);
            return exclusive ? (compare >= 0) : (compare > 0);
        }
        
        private boolean isInvalidMax(final String maxField, final boolean exclusive) {
            final NumberFieldValidator<?> v = (NumberFieldValidator<?>) validator.fieldValidators.get(validator.prefix + maxField);
            final int compare = NumberHelper.compare(value, v.value);
            return exclusive ? (compare >= 0) : (compare > 0);
        }
        
        public NumberFieldValidator<V> in(final Number... allowed) {
            return in(Arrays.asList(allowed));
        }
        
        public NumberFieldValidator<V> in(final Collection<Number> allowed) {
            if (present && allowed.stream().noneMatch(n -> NumberHelper.compare(value, n) == 0)) {
                validator.invalid(name, FieldValidation.Type.IN, allowed.stream().map(Object::toString).collect(Collectors.toList()));
            }
            
            return this;
        }
        
        public NumberFieldValidator<V> notIn(final Number... notAllowed) {
            return in(Arrays.asList(notAllowed));
        }
        
        public NumberFieldValidator<V> notIn(final Collection<Number> notAllowed) {
            if (present && notAllowed.stream().anyMatch(n -> NumberHelper.compare(value, n) == 0)) {
                validator.invalid(name, FieldValidation.Type.IN, notAllowed.stream().map(Object::toString).collect(Collectors.toList()));
            }
            
            return this;
        }
        
        @Override
        public V getValue() {
            return value;
        }
    }
    
    public static final class NumberListValidator<V extends Number> extends ListValidator<V, NumberListValidator<V>> {
        
        private NumberListValidator(final RootValidator validator, final String name, final List<V> list) {
            super(validator, name, list);
        }
        
        public NumberListValidator<V> values(final ListItemCallback<NumberFieldValidator<V>> callback) {
            
            if (present) {
                int i = 0;
                for (final V item : list) {
                    callback.onItem(validator.field(name + "[" + i++ + "]", item));
                }
            }
            
            return this;
        }
    }
    
    public static final class NumberMapNumberKeyValidator<K extends Number, V extends Number> extends MapNumberKeyValidator<K, V, NumberMapNumberKeyValidator<K, V>> {
        
        private NumberMapNumberKeyValidator(final RootValidator validator, final String name, final Map<K, V> map) {
            super(validator, name, map);
        }
        
        public NumberMapNumberKeyValidator<K, V> entries(final MapEntryCallback<NumberFieldValidator<K>, NumberFieldValidator<V>> callback) {
            
            if (present) {
                for (final Map.Entry<K, V> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class NumberMapStringKeyValidator<V extends Number> extends MapStringKeyValidator<V, NumberMapStringKeyValidator<V>> {
        
        private NumberMapStringKeyValidator(final RootValidator validator, final String name, final Map<String, V> map) {
            super(validator, name, map);
        }
        
        public NumberMapStringKeyValidator<V> entries(final MapEntryCallback<StringFieldValidator, NumberFieldValidator<V>> callback) {
            
            if (present) {
                for (final Map.Entry<String, V> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class NumberMapEnumKeyValidator<V extends Number, E extends Enum<E> & ProtocolMessageEnum> extends MapStringKeyValidator<V, NumberMapEnumKeyValidator<V, E>> {
        
        private final Class<E> type;
        
        private NumberMapEnumKeyValidator(final RootValidator validator, final String name, final Class<E> type, final Map<String, V> map) {
            super(validator, name, map);
            this.type = type;
        }
        
        public NumberMapEnumKeyValidator<V, E> entries(final MapEntryCallback<EnumFieldValidator<E>, NumberFieldValidator> callback) {
            
            if (present) {
                for (final Map.Entry<String, V> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), validateMapEnumKey(type, entry.getKey())),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
        
    }
    
    public static final class BooleanFieldValidator extends FieldValidator<BooleanFieldValidator> {
        
        private final boolean value;
        
        private BooleanFieldValidator(final RootValidator validator, final String name, final boolean present, final boolean value) {
            super(validator, name, present);
            this.value = value;
        }
        
        @Override
        public Object getValue() {
            return value;
        }
    }
    
    public static final class BooleanListValidator extends ListValidator<Boolean, BooleanListValidator> {
        
        private BooleanListValidator(final RootValidator validator, final String name, final List<Boolean> list) {
            super(validator, name, list);
        }
        
        public BooleanListValidator values(final ListItemCallback<BooleanFieldValidator> callback) {
            
            if (present) {
                int i = 0;
                for (final boolean item : list) {
                    callback.onItem(validator.field(name + "[" + i++ + "]", item));
                }
            }
            
            return this;
        }
    }
    
    public static final class BooleanMapNumberKeyValidator<K extends Number> extends MapNumberKeyValidator<K, Boolean, BooleanMapNumberKeyValidator<K>> {
        
        private BooleanMapNumberKeyValidator(final RootValidator validator, final String name, final Map<K, Boolean> map) {
            super(validator, name, map);
        }
        
        public BooleanMapNumberKeyValidator<K> entries(final MapEntryCallback<NumberFieldValidator<K>, BooleanFieldValidator> callback) {
            
            if (present) {
                for (final Map.Entry<K, Boolean> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class BooleanMapStringKeyValidator extends MapStringKeyValidator<Boolean, BooleanMapStringKeyValidator> {
        
        private BooleanMapStringKeyValidator(final RootValidator validator, final String name, final Map<String, Boolean> map) {
            super(validator, name, map);
        }
        
        public BooleanMapStringKeyValidator entries(final MapEntryCallback<StringFieldValidator, BooleanFieldValidator> callback) {
            
            if (present) {
                for (final Map.Entry<String, Boolean> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class BooleanMapEnumKeyValidator<E extends Enum<E> & ProtocolMessageEnum> extends MapStringKeyValidator<Boolean, BooleanMapEnumKeyValidator<E>> {
        
        private final Class<E> type;
        
        private BooleanMapEnumKeyValidator(final RootValidator validator, final String name, final Class<E> type, final Map<String, Boolean> map) {
            super(validator, name, map);
            this.type = type;
        }
        
        public BooleanMapEnumKeyValidator<E> entries(final MapEntryCallback<EnumFieldValidator<E>, BooleanFieldValidator> callback) {
            
            if (present) {
                for (final Map.Entry<String, Boolean> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), validateMapEnumKey(type, entry.getKey())),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
        
    }
    
    public static final class StringFieldValidator extends FieldValidator<StringFieldValidator> {
        
        private static final Map<String, Pattern> PATTERNS = new HashMap<>();
        private static final StampedLock PATTERNS_LOCK = new StampedLock();
        
        private static Pattern getOrCreatePattern(final String pattern) {
            return LockUtil.readMaybeWrite(PATTERNS_LOCK,
                true,
                () -> PATTERNS.get(pattern),
                Objects::nonNull,
                () -> PATTERNS.computeIfAbsent(pattern, Pattern::compile));
        }
        
        private final String value;
        
        private StringFieldValidator(final RootValidator validator, final String name, final String value) {
            super(validator, name, !value.equals(""));
            this.value = value;
        }
        
        public StringFieldValidator hasText() {
            
            if (present && value.trim().equals("")) {
                validator.invalid(name, FieldValidation.Type.HAS_TEXT);
            }
            
            return this;
        }
        
        @SuppressWarnings("unchecked")
        public StringFieldValidator size(final Integer min, final Integer max) {
            if (present) {
                boolean invalid = false;
                if ((min != null) && (value.length() < min)) {
                    invalid = true;
                }
                if (!invalid && (max != null) && (value.length() > max)) {
                    invalid = true;
                }
                if (invalid) {
                    validator.invalid(name, FieldValidation.Type.SIZE, min != null ? min.toString() : "", max != null ? max.toString() : "");
                }
            }
            return this;
        }
        
        public StringFieldValidator regex(final String pattern) {
            return regex(getOrCreatePattern(pattern));
        }
        
        public StringFieldValidator regex(final Pattern pattern) {
            if (present && !pattern.matcher(value).matches()) {
                validator.invalid(name, FieldValidation.Type.REGEX, pattern.toString());
            }
            
            return this;
        }
        
        public StringFieldValidator in(final String... allowed) {
            return in(Arrays.asList(allowed));
        }
        
        public StringFieldValidator in(final Collection<String> allowed) {
            if (present && !allowed.contains(value)) {
                validator.invalid(name, FieldValidation.Type.IN, allowed);
            }
            
            return this;
        }
        
        public StringFieldValidator notIn(final String... notAllowed) {
            return notIn(Arrays.asList(notAllowed));
        }
        
        public StringFieldValidator notIn(final Collection<String> notAllowed) {
            if (present && notAllowed.contains(value)) {
                validator.invalid(name, FieldValidation.Type.NOT_IN, notAllowed);
            }
            
            return this;
        }
        
        @Override
        public Object getValue() {
            return value;
        }
    }
    
    public static final class StringListValidator extends ListValidator<String, StringListValidator> {
        
        private StringListValidator(final RootValidator validator, final String name, final List<String> list) {
            super(validator, name, list);
        }
        
        public StringListValidator values(final ListItemCallback<StringFieldValidator> callback) {
            
            if (present) {
                int i = 0;
                for (final String item : list) {
                    callback.onItem(validator.field(name + "[" + i++ + "]", item));
                }
            }
            
            return this;
        }
    }
    
    public static final class StringMapNumberKeyValidator<K extends Number> extends MapValidator<K, String, StringMapNumberKeyValidator<K>> {
        
        private StringMapNumberKeyValidator(final RootValidator validator, final String name, final Map<K, String> map) {
            super(validator, name, map);
        }
        
        public StringMapNumberKeyValidator<K> entries(final MapEntryCallback<NumberFieldValidator<K>, StringFieldValidator> callback) {
            
            if (present) {
                for (final Map.Entry<K, String> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class StringMapStringKeyValidator extends MapStringKeyValidator<String, StringMapStringKeyValidator> {
        
        private StringMapStringKeyValidator(final RootValidator validator, final String name, final Map<String, String> map) {
            super(validator, name, map);
        }
        
        public StringMapStringKeyValidator entries(final MapEntryCallback<StringFieldValidator, StringFieldValidator> callback) {
            
            if (present) {
                for (final Map.Entry<String, String> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class StringMapEnumKeyValidator<E extends Enum<E> & ProtocolMessageEnum> extends MapStringKeyValidator<String, StringMapEnumKeyValidator<E>> {
        
        private final Class<E> type;
        
        private StringMapEnumKeyValidator(final RootValidator validator, final String name, final Class<E> type, final Map<String, String> map) {
            super(validator, name, map);
            this.type = type;
        }
        
        public StringMapEnumKeyValidator<E> entries(final MapEntryCallback<EnumFieldValidator<E>, StringFieldValidator> callback) {
            
            if (present) {
                for (final Map.Entry<String, String> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), validateMapEnumKey(type, entry.getKey())),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
        
    }
    
    public static final class BytesFieldValidator extends FieldValidator<BytesFieldValidator> {
        
        private final ByteString value;
        
        private BytesFieldValidator(final RootValidator validator, final String name, final ByteString value) {
            super(validator, name, (value != null) && !value.isEmpty());
            this.value = value;
        }
        
        @Override
        public Object getValue() {
            return value;
        }
    }
    
    public static final class BytesListValidator extends ListValidator<ByteString, BytesListValidator> {
        
        private BytesListValidator(final RootValidator validator, final String name, final List<ByteString> list) {
            super(validator, name, list);
        }
        
        public BytesListValidator values(final ListItemCallback<BytesFieldValidator> callback) {
            
            if (present) {
                int i = 0;
                for (final ByteString item : list) {
                    callback.onItem(validator.field(name + "[" + i++ + "]", item));
                }
            }
            
            return this;
        }
    }
    
    public static final class BytesMapNumberKeyValidator<K extends Number> extends MapValidator<K, ByteString, BytesMapNumberKeyValidator<K>> {
        
        private BytesMapNumberKeyValidator(final RootValidator validator, final String name, final Map<K, ByteString> map) {
            super(validator, name, map);
        }
        
        public BytesMapNumberKeyValidator<K> entries(final MapEntryCallback<NumberFieldValidator<K>, BytesFieldValidator> callback) {
            
            if (present) {
                for (final Map.Entry<K, ByteString> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class BytesMapStringKeyValidator extends MapStringKeyValidator<ByteString, BytesMapStringKeyValidator> {
        
        private BytesMapStringKeyValidator(final RootValidator validator, final String name, final Map<String, ByteString> map) {
            super(validator, name, map);
        }
        
        public BytesMapStringKeyValidator entries(final MapEntryCallback<StringFieldValidator, BytesFieldValidator> callback) {
            
            if (present) {
                for (final Map.Entry<String, ByteString> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class BytesMapEnumKeyValidator<E extends Enum<E> & ProtocolMessageEnum> extends MapStringKeyValidator<ByteString, BytesMapEnumKeyValidator<E>> {
        
        private final Class<E> type;
        
        private BytesMapEnumKeyValidator(final RootValidator validator, final String name, final Class<E> type, final Map<String, ByteString> map) {
            super(validator, name, map);
            this.type = type;
        }
        
        public BytesMapEnumKeyValidator<E> entries(final MapEntryCallback<EnumFieldValidator<E>, BytesFieldValidator> callback) {
            
            if (present) {
                for (final Map.Entry<String, ByteString> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), validateMapEnumKey(type, entry.getKey())),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
        
    }
    
    public static final class MessageFieldValidator<T extends MessageLite> extends FieldValidator<MessageFieldValidator<T>> {
        
        private final T value;
        
        private MessageFieldValidator(final RootValidator validator, final String name, final T value) {
            super(validator, name, value != value.getDefaultInstanceForType());
            this.value = value;
            if (present) {
                resolveValidationConsumer(value).accept(value, new RootValidator(validator.prefix + name + ".", validator));
            }
        }
        
        @Override
        public Object getValue() {
            return value;
        }
    }
    
    public static final class MessageListValidator<T extends MessageLite> extends ListValidator<T, MessageListValidator<T>> {
        
        private MessageListValidator(final RootValidator validator, final String name, final List<T> list) {
            super(validator, name, list);
        }
        
        public MessageListValidator<T> values(final ListItemCallback<MessageFieldValidator<T>> callback) {
            
            if (present) {
                int i = 0;
                for (final T item : list) {
                    callback.onItem(validator.field(name + "[" + i++ + "]", item));
                }
            }
            
            return this;
        }
    }
    
    public static final class MessageMapNumberKeyValidator<K extends Number, T extends MessageLite> extends MapNumberKeyValidator<K, T, MessageMapNumberKeyValidator<K, T>> {
        
        private MessageMapNumberKeyValidator(final RootValidator validator, final String name, final Map<K, T> map) {
            super(validator, name, map);
        }
        
        public MessageMapNumberKeyValidator<K, T> entries(final MapEntryCallback<NumberFieldValidator<K>, MessageFieldValidator<T>> callback) {
            
            if (present) {
                for (final Map.Entry<K, T> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class MessageMapStringKeyValidator<T extends MessageLite> extends MapStringKeyValidator<T, MessageMapStringKeyValidator<T>> {
        
        private MessageMapStringKeyValidator(final RootValidator validator, final String name, final Map<String, T> map) {
            super(validator, name, map);
        }
        
        public MessageMapStringKeyValidator<T> entries(final MapEntryCallback<StringFieldValidator, MessageFieldValidator<T>> callback) {
            
            if (present) {
                for (final Map.Entry<String, T> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class MessageMapEnumKeyValidator<T extends MessageLite, E extends Enum<E> & ProtocolMessageEnum> extends MapStringKeyValidator<T, MessageMapEnumKeyValidator<T, E>> {
        
        private final Class<E> type;
        
        private MessageMapEnumKeyValidator(final RootValidator validator, final String name, final Class<E> type, final Map<String, T> map) {
            super(validator, name, map);
            this.type = type;
        }
        
        public MessageMapEnumKeyValidator<T, E> entries(final MapEntryCallback<EnumFieldValidator<E>, MessageFieldValidator<T>> callback) {
            
            if (present) {
                for (final Map.Entry<String, T> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), validateMapEnumKey(type, entry.getKey())),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
        
    }
    
    public static final class EnumFieldValidator<T extends Enum<T> & ProtocolMessageEnum> extends FieldValidator<EnumFieldValidator<T>> {
        
        private final T value;
        
        private EnumFieldValidator(final RootValidator validator, final String name, final T value) {
            super(validator, name, (value != null) && !value.toString().equals("UNRECOGNIZED") && value.getNumber() > 0);
            this.value = value;
            if ((value == null) || value.toString().equals("UNRECOGNIZED")) {
                validator.invalid(name, FieldValidation.Type.TYPE);
            }
        }
        
        @SafeVarargs
        public final EnumFieldValidator<T> in(final T... allowed) {
            return in(Arrays.asList(allowed));
        }
        
        public final EnumFieldValidator<T> in(final Collection<T> allowed) {
            if (present && allowed.stream().noneMatch(o -> o.getNumber() == value.getNumber())) {
                validator.invalid(name, FieldValidation.Type.IN, allowed.stream().map(Object::toString).collect(Collectors.toList()));
            }
            
            return this;
        }
        
        @SafeVarargs
        public final EnumFieldValidator<T> notIn(final T... notAllowed) {
            return notIn(Arrays.asList(notAllowed));
        }
        
        public final EnumFieldValidator<T> notIn(final Collection<T> notAllowed) {
            if (present && notAllowed.stream().anyMatch(o -> o.getNumber() == value.getNumber())) {
                validator.invalid(name, FieldValidation.Type.NOT_IN, notAllowed.stream().map(Object::toString).collect(Collectors.toList()));
            }
            
            return this;
        }
        
        @Override
        public Object getValue() {
            return value;
        }
    }
    
    public static final class EnumListValidator<T extends Enum<T> & ProtocolMessageEnum> extends ListValidator<T, EnumListValidator<T>> {
        
        private EnumListValidator(final RootValidator validator, final String name, final List<T> list) {
            super(validator, name, list);
        }
        
        public EnumListValidator<T> values(final ListItemCallback<EnumFieldValidator<T>> callback) {
            
            if (present) {
                int i = 0;
                for (final T item : list) {
                    callback.onItem(validator.field(name + "[" + i++ + "]", item));
                }
            }
            
            return this;
        }
    }
    
    public static final class EnumMapNumberKeyValidator<K extends Number, T extends Enum<T> & ProtocolMessageEnum> extends MapNumberKeyValidator<K, T, EnumMapNumberKeyValidator<K, T>> {
        
        private EnumMapNumberKeyValidator(final RootValidator validator, final String name, final Map<K, T> map) {
            super(validator, name, map);
        }
        
        public EnumMapNumberKeyValidator<K, T> entries(final MapEntryCallback<NumberFieldValidator<K>, EnumFieldValidator<T>> callback) {
            
            if (present) {
                for (final Map.Entry<K, T> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    public static final class EnumMapStringKeyValidator<T extends Enum<T> & ProtocolMessageEnum> extends MapStringKeyValidator<T, EnumMapStringKeyValidator<T>> {
        
        private EnumMapStringKeyValidator(final RootValidator validator, final String name, final Map<String, T> map) {
            super(validator, name, map);
        }
        
        public EnumMapStringKeyValidator<T> entries(final MapEntryCallback<StringFieldValidator, EnumFieldValidator<T>> callback) {
            
            if (present) {
                for (final Map.Entry<String, T> entry : map.entrySet()) {
                    callback.onEntry(validator.field(name + "[" + entry.getKey(), entry.getKey()),
                        validator.field(name + "[" + entry.getKey() + "]", entry.getValue()));
                }
            }
            
            return this;
        }
    }
    
    private static Iterable<String> join(final Number n, final String... fields) {
        final List<String> joined = new ArrayList<>(fields.length + 1);
        joined.add(n.toString());
        joined.addAll(Arrays.asList(fields));
        return joined;
    }
    
    private static <E extends Enum<E> & ProtocolMessageEnum> E validateMapEnumKey(final Class<E> type, final String key) {
        try {
            return Enum.valueOf(type, key);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }
    
    private MessageValidator() {}
    
}
