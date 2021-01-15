/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.Part;
import com.ixaris.commons.dimensions.lib.CommonsDimensionsLib.KeyName;

/**
 * Represents a property, list, counter or context dimension value.
 *
 * <p>All subclasses should have a no-args constructor so that the context properties module can construct them using reflection. There is no way
 * to enforce this so it is very important that subclasses follow this rule.
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public abstract class Value implements Serializable {
    
    public static <T extends Value> CommonsDimensionsConfig.Set setToProtobuf(final Set<T> set) {
        final CommonsDimensionsConfig.Set.Builder builder = CommonsDimensionsConfig.Set.newBuilder();
        for (final T value : set) {
            builder.addValues(value.toProtobuf());
        }
        return builder.build();
    }
    
    public static List<Part> toProtobuf(final Value.Builder<?> valueBuilder) {
        final List<Part> parts = new ArrayList<>();
        for (int i = 0; i < valueBuilder.getNumberOfParts(); i++) {
            final Part.Builder partBuilder = Part.newBuilder().setName(valueBuilder.getPartName(i));
            if (valueBuilder.isPartFixedValue(i)) {
                for (final Entry<String, String> entry : valueBuilder.getPartFixedValues(i).entrySet()) {
                    partBuilder.addFixedValues(KeyName.newBuilder().setKey(entry.getKey()).setName(entry.getValue()));
                }
            }
            parts.add(partBuilder.build());
        }
        return parts;
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Value> Builder<T> getBuilderForType(final Class<T> valueClass) {
        try {
            final Method newBuilder = valueClass.getMethod("newBuilder");
            return (Builder<T>) newBuilder.invoke(null);
        } catch (final Exception e) {
            throw new IllegalStateException("Builder for type " + valueClass + " cannot be created", e);
        }
    }
    
    public interface Builder<T extends Value> {
        
        /**
         * @return the number of distinct parts that make up this value. By default 1. Should be changed only for multi-part values
         */
        default int getNumberOfParts() {
            return 1;
        }
        
        /**
         * @param part zero-based
         * @return true if part had fixed values
         */
        default boolean isPartFixedValue(final int part) {
            return false;
        }
        
        /**
         * @param part zero-based
         * @return a friendly name for the part, by default "Value" if there is only one part or "Part 1" for part index 0 etc.
         */
        default String getPartName(final int part) {
            if ((part == 0) && (getNumberOfParts() == 1)) {
                return "Value";
            } else {
                return "Part " + (part + 1);
            }
        }
        
        /**
         * Get the enumerated list of possible values for this part. Default implementation assumes the no part is enumerable.
         *
         * @param part
         * @return the enumerated list
         * @throws UnsupportedOperationException
         */
        default Map<String, String> getPartFixedValues(final int part) {
            throw new UnsupportedOperationException();
        }
        
        /**
         * Created the value from a persisted value
         *
         * @param persistedValue
         */
        T buildFromPersisted(PersistedValue persistedValue);
        
        /**
         * Creates this value from a number of string parts
         *
         * @param parts
         * @throws IllegalArgumentException
         */
        T buildFromStringParts(String... parts);
        
        default T buildFromStringParts(final List<String> parts) {
            return buildFromStringParts(parts.toArray(new String[0]));
        }
        
        default T buildFromProtobuf(final CommonsDimensionsConfig.Value value) {
            return buildFromStringParts(value.getPartsList());
        }
        
    }
    
    /**
     * No-args constructor for creating the value by reflection. Should be available in all subclasses
     */
    protected Value() {}
    
    /**
     * @return the persisted representation of this value
     */
    public abstract PersistedValue getPersistedValue();
    
    /**
     * @return the value as string parts
     */
    public abstract String[] getStringParts();
    
    public final CommonsDimensionsConfig.Value toProtobuf() {
        return CommonsDimensionsConfig.Value.newBuilder().addAllParts(Arrays.asList(getStringParts())).build();
    }
    
    /**
     * Check for equality on the basis of dimension name and value
     *
     * @param o the object with which to check equality
     * @return true if the given object is equal to this, false otherwise
     */
    @Override
    public abstract boolean equals(final Object o);
    
    /**
     * Generates a hashcode on the basis of property name and value.
     *
     * <p>Maintains the general contract that equal objects must have equal hash codes.
     *
     * @return the hashcode
     */
    @Override
    public abstract int hashCode();
    
}
