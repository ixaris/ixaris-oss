package com.ixaris.commons.protobuf.lib;

import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.NullableBoolean;
import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.NullableDouble;
import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.NullableInt32;
import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.NullableInt64;
import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.Paging;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Stream;

/**
 * Converter class for basic messages
 *
 * @author <a href="mailto:armand.sciberras@ixaris.com">armand.sciberras</a>
 */
public class ProtobufConverters {
    
    private static final int DEFAULT_LIMIT = 100;
    
    public static int getDefaultLimit() {
        return DEFAULT_LIMIT;
    }
    
    public static <T> Stream<T> applyPaging(final Stream<T> stream, final Paging paging) {
        Stream<T> s = stream;
        if (paging.getOffset() > 0) {
            s = s.skip((long) paging.getOffset());
        }
        if (paging.getLimit() > 0) {
            s = s.limit((long) paging.getLimit());
        } else {
            s = s.limit(DEFAULT_LIMIT);
        }
        return s;
    }
    
    @SuppressWarnings("squid:S2447")
    public static Boolean toBoolean(final NullableBoolean n) {
        switch (n) {
            case TRUE:
                return Boolean.TRUE;
            case FALSE:
                return Boolean.FALSE;
            case NULL:
            case UNRECOGNIZED:
            default:
                return null;
        }
    }
    
    public static Integer toInteger(final NullableInt32 n) {
        if (n.getValue() == 0 && !n.getHasValue()) {
            return null;
        } else {
            return n.getValue();
        }
    }
    
    public static Long toLong(final NullableInt64 n) {
        if (n.getValue() == 0L && !n.getHasValue()) {
            return null;
        } else {
            return n.getValue();
        }
    }
    
    @SuppressWarnings("squid:S1244")
    public static Double toDouble(final NullableDouble n) {
        if (n.getValue() == 0.0 && !n.getHasValue()) {
            return null;
        } else {
            return n.getValue();
        }
    }
    
    public static Optional<Boolean> toOptional(final NullableBoolean n) {
        return Optional.ofNullable(toBoolean(n));
    }
    
    public static OptionalInt toOptional(final NullableInt32 n) {
        if (n.getValue() == 0 && !n.getHasValue()) {
            return OptionalInt.empty();
        } else {
            return OptionalInt.of(n.getValue());
        }
    }
    
    public static OptionalLong toOptional(final NullableInt64 n) {
        if (n.getValue() == 0L && !n.getHasValue()) {
            return OptionalLong.empty();
        } else {
            return OptionalLong.of(n.getValue());
        }
    }
    
    @SuppressWarnings("squid:S1244")
    public static OptionalDouble toOptional(final NullableDouble n) {
        if (n.getValue() == 0.0 && !n.getHasValue()) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(n.getValue());
        }
    }
    
    public static NullableBoolean convert(final Boolean value) {
        if (value == null) {
            return NullableBoolean.NULL;
        } else if (value) {
            return NullableBoolean.TRUE;
        } else {
            return NullableBoolean.FALSE;
        }
    }
    
    public static NullableInt32 convert(final Integer value) {
        return NullableInt32.newBuilder().setHasValue(value != null).setValue(value == null ? 0 : value).build();
    }
    
    public static NullableInt32 convert(final OptionalInt value) {
        return NullableInt32.newBuilder().setHasValue(value.isPresent()).setValue(value.orElse(0)).build();
    }
    
    public static NullableInt64 convert(final Long value) {
        return NullableInt64.newBuilder().setHasValue(value != null).setValue(value == null ? 0L : value).build();
    }
    
    public static NullableInt64 convert(final OptionalLong value) {
        return NullableInt64.newBuilder().setHasValue(value.isPresent()).setValue(value.orElse(0L)).build();
    }
    
    public static NullableDouble convert(final Double value) {
        return NullableDouble.newBuilder().setHasValue(value != null).setValue(value == null ? 0.0 : value).build();
    }
    
    public static NullableDouble convert(final OptionalDouble value) {
        return NullableDouble.newBuilder().setHasValue(value.isPresent()).setValue(value.orElse(0.0)).build();
    }
    
    private ProtobufConverters() {}
    
}
