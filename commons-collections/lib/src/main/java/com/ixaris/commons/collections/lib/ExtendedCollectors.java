package com.ixaris.commons.collections.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;

public class ExtendedCollectors {
    
    public static <T, K, U> Collector<T, ?, Map<K, List<U>>> toMapOfList(
        final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends U> valueMapper
    ) {
        return Collector.of(
            HashMap::new,
            (map, element) ->
                addToMapOfList(
                    map,
                    keyMapper.apply(element),
                    Collections.singletonList(Objects.requireNonNull(valueMapper.apply(element)))
                ),
            (m1, m2) -> {
                for (final Entry<K, List<U>> e : m2.entrySet()) {
                    addToMapOfList(m1, e.getKey(), Objects.requireNonNull(e.getValue()));
                }
                return m1;
            }
        );
    }
    
    private static <K, U> void addToMapOfList(final Map<K, List<U>> map, final K k, final List<U> value) {
        map.compute(k, (kk, v) -> {
            if (v == null) {
                return new ArrayList<>(value);
            } else {
                v.addAll(value);
                return v;
            }
        });
    }
    
    public static <T, K1, K2, U> Collector<T, ?, Map<K1, Map<K2, List<U>>>> toMapOfMapOfList(
        final Function<? super T, ? extends K1> key1Mapper,
        final Function<? super T, ? extends K2> key2Mapper,
        final Function<? super T, ? extends U> valueMapper
    ) {
        return Collector.of(
            HashMap::new,
            (map, element) ->
                addToMapOfMapOfList(
                    map,
                    key1Mapper.apply(element),
                    key2Mapper.apply(element),
                    Collections.singletonList(Objects.requireNonNull(valueMapper.apply(element)))
                ),
            (m1, m2) -> {
                for (final Entry<K1, Map<K2, List<U>>> me : m2.entrySet()) {
                    final K1 k1 = me.getKey();
                    for (final Entry<K2, List<U>> e : me.getValue().entrySet()) {
                        addToMapOfMapOfList(m1, k1, e.getKey(), Objects.requireNonNull(e.getValue()));
                    }
                }
                return m1;
            }
        );
    }
    
    private static <K1, K2, U> void addToMapOfMapOfList(
        final Map<K1, Map<K2, List<U>>> map, final K1 k1, final K2 k2, final List<U> value
    ) {
        map.compute(k1, (kk1, v1) -> {
            if (v1 == null) {
                return ExtendedCollections.buildMap(new HashMap<>(), k2, new ArrayList<>(value));
            } else {
                v1.compute(k2, (kk2, v2) -> {
                    if (v2 == null) {
                        return new ArrayList<>(value);
                    } else {
                        v2.addAll(value);
                        return v2;
                    }
                });
                return v1;
            }
        });
    }
    
    public static <T, U> Collector<T, ?, Optional<U>> reducing(
        final Function<T, U> initiator, final BiFunction<U, T, U> op
    ) {
        
        return reducing(initiator, op, (u1, u2) -> {
            throw new UnsupportedOperationException("To use with parallel streams, specify a combiner");
        });
    }
    
    public static <T, U> Collector<T, ?, Optional<U>> reducing(
        final Function<T, U> initiator, final BiFunction<U, T, U> op, final BinaryOperator<U> combiner
    ) {
        
        class OptionalBox implements Consumer<T> {
            
            private U value;
            private boolean present;
            
            public OptionalBox() {}
            
            public OptionalBox(final U value) {
                this.value = value;
                present = value != null;
            }
            
            @Override
            public void accept(final T t) {
                if (present) {
                    value = op.apply(value, t);
                } else {
                    value = initiator.apply(t);
                    present = true;
                }
            }
        }
        
        return Collector.of(
            OptionalBox::new,
            OptionalBox::accept,
            (a, b) -> {
                if (b.present) {
                    if (a.present) {
                        return new OptionalBox(combiner.apply(a.value, b.value));
                    } else {
                        return b;
                    }
                } else {
                    return a;
                }
            },
            a -> Optional.ofNullable(a.value)
        );
    }
    
    public static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }
    
    private ExtendedCollectors() {}
    
}
