package com.ixaris.commons.protobuf.lib;

import static org.assertj.core.api.Assertions.assertThat;

import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.NullableDouble;
import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.NullableInt64;
import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.Paging;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class ProtobufConvertersTest {
    
    @Test
    public void convertNullableLongToOptional_noHasValueNonZero_shouldReturnCorrectNonZeroValue() {
        final OptionalLong od = ProtobufConverters.toOptional(NullableInt64.newBuilder().setValue(1L).build());
        assertThat(od).isPresent().hasValue(1L);
    }
    
    @Test
    public void convertNullableLongToOptional_noHasValueAndZero_shouldReturnEmptyOptional() {
        final OptionalLong od = ProtobufConverters.toOptional(NullableInt64.newBuilder().build());
        assertThat(od).isNotPresent();
    }
    
    @Test
    public void convertNullableLongToOptional_hasValueAndZero_shouldReturnZeroValue() {
        final OptionalLong od = ProtobufConverters.toOptional(NullableInt64.newBuilder().setHasValue(true).build());
        assertThat(od).isPresent().hasValue(0L);
    }
    
    @Test
    public void convertNullableDoubleToOptional_noHasValueNonZero_shouldReturnCorrectNonZeroValue() {
        final OptionalDouble od = ProtobufConverters.toOptional(NullableDouble.newBuilder().setValue(1.0).build());
        assertThat(od).isPresent().hasValue(1.0);
    }
    
    @Test
    public void convertNullableDoubleToOptional_noHasValueAndZero_shouldReturnEmptyOptional() {
        final OptionalDouble od = ProtobufConverters.toOptional(NullableDouble.newBuilder().build());
        assertThat(od).isNotPresent();
    }
    
    @Test
    public void convertNullableDoubleToOptional_hasValueAndZero_shouldReturnZeroValue() {
        final OptionalDouble od = ProtobufConverters.toOptional(NullableDouble.newBuilder().setHasValue(true).build());
        assertThat(od).isPresent().hasValue(0.0);
    }
    
    @Test
    public void applyPaging_pagingOffsetSet_shouldReturnOffsetStream() {
        final long startValue = 1L;
        final int offset = 2;
        final long startPlusOffset = 3L;
        
        final Stream<Long> actual = ProtobufConverters.applyPaging(
            createRangeStream(startValue, ProtobufConverters.getDefaultLimit()),
            Paging.newBuilder().setOffset(offset).build()
        );
        assertThat(actual.collect(Collectors.toList())).isEqualTo(
            createRangeStream(startPlusOffset, ProtobufConverters.getDefaultLimit()).collect(Collectors.toList())
        );
    }
    
    @Test
    public void applyPaging_pagingLimitSet_shouldReturnLimitedStream() {
        final long startValue = 1L;
        final long endValue = 200L;
        final int limit = 20;
        
        final Stream<Long> actual = ProtobufConverters.applyPaging(
            createRangeStream(startValue, endValue), Paging.newBuilder().setLimit(limit).build()
        );
        assertThat(actual.collect(Collectors.toList())).isEqualTo(
            createRangeStream(startValue, limit).collect(Collectors.toList())
        );
    }
    
    @Test
    public void applyPaging_noPagingLimitSet_shouldReturnLimitedStreamByDefaultValue() {
        final long startValue = 1L;
        final long endValue = 200L;
        
        final Stream<Long> actual = ProtobufConverters.applyPaging(
            createRangeStream(startValue, endValue), Paging.newBuilder().build()
        );
        assertThat(actual.collect(Collectors.toList())).isEqualTo(
            createRangeStream(startValue, ProtobufConverters.getDefaultLimit()).collect(Collectors.toList())
        );
    }
    
    @Test
    public void applyPaging_pagingOffsetAndLimitSet_shouldReturnOffsetAndLimitedStream() {
        final long startValue = 1L;
        final long endValue = 200L;
        final int limit = 20;
        final int offset = 2;
        final long startPlusOffset = 3L;
        final long limitPlusOffset = 22L;
        
        final Stream<Long> actual = ProtobufConverters.applyPaging(
            createRangeStream(startValue, endValue), Paging.newBuilder().setLimit(limit).setOffset(offset).build()
        );
        assertThat(actual.collect(Collectors.toList())).isEqualTo(
            createRangeStream(startPlusOffset, limitPlusOffset).collect(Collectors.toList())
        );
    }
    
    private static Stream<Long> createRangeStream(final long startInclusive, final long endInclusive) {
        return LongStream.range(startInclusive, endInclusive + 1L).boxed();
    }
}
