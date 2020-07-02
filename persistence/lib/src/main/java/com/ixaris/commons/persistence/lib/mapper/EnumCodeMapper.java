package com.ixaris.commons.persistence.lib.mapper;

import java.util.Arrays;
import java.util.function.Function;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import com.ixaris.commons.collections.lib.GuavaCollectors;

/**
 * Enum to a Code mapper used to map enums to code values (generally character values) and vice versa.
 *
 * @author armand.sciberras
 */
public class EnumCodeMapper<E extends Enum<E>, C> {
    
    public static <E extends Enum<E>, C> EnumCodeMapper<E, C> with(final ImmutableBiMap<E, C> mapping) {
        return new EnumCodeMapper<>(mapping);
    }
    
    public static <E extends Enum<E>> EnumCodeMapper<E, String> byName(final E[] values) {
        return usingMapper(values, Enum::name);
    }
    
    public static <E extends Enum<E>, C> EnumCodeMapper<E, C> usingMapper(final E[] values, final Function<E, C> mappingFunction) {
        return EnumCodeMapper.with(Arrays.stream(values).collect(GuavaCollectors.toImmutableBiMap(Function.identity(), mappingFunction)));
    }
    
    private final BiMap<E, C> mapping;
    
    private EnumCodeMapper(final ImmutableBiMap<E, C> mapping) {
        this.mapping = mapping;
    }
    
    public E resolve(final C code) {
        final E resolved = mapping.inverse().get(code);
        if (resolved == null) {
            throw new IllegalArgumentException(String.format("Unable to resolve mapping for code: [%s]", code));
        } else {
            return resolved;
        }
    }
    
    public C codify(final E item) {
        final C codified = mapping.get(item);
        if (codified == null) {
            throw new IllegalArgumentException(String.format("Unable to resolve mapping for value: [%s]", item));
        } else {
            return codified;
        }
    }
    
}
