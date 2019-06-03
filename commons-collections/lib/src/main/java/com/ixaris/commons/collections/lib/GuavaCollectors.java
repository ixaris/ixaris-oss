package com.ixaris.commons.collections.lib;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.function.Function;
import java.util.stream.Collector;

public final class GuavaCollectors {
    
    public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toImmutableList() {
        return Collector.of(
            ImmutableList.Builder::new,
            ImmutableList.Builder::add,
            (l, r) -> l.addAll(r.build()),
            ImmutableList.Builder::build
        );
    }
    
    public static <T> Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> toImmutableSet() {
        return Collector.of(
            ImmutableSet.Builder::new,
            ImmutableSet.Builder::add,
            (l, r) -> l.addAll(r.build()),
            ImmutableSet.Builder::build
        );
    }
    
    public static <T, K, V> Collector<T, ImmutableMap.Builder<K, V>, ImmutableMap<K, V>> toImmutableMap(
        final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper
    ) {
        return Collector.of(
            ImmutableMap.Builder::new,
            (a, t) -> a.put(keyMapper.apply(t), valueMapper.apply(t)),
            (l, r) -> l.putAll(r.build()),
            ImmutableMap.Builder::build
        );
    }
    
    public static <T, K, V> Collector<T, ImmutableBiMap.Builder<K, V>, ImmutableBiMap<K, V>> toImmutableBiMap(
        final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper
    ) {
        return Collector.of(
            ImmutableBiMap.Builder::new,
            (a, t) -> a.put(keyMapper.apply(t), valueMapper.apply(t)),
            (l, r) -> l.putAll(r.build()),
            ImmutableBiMap.Builder::build
        );
    }
    
    private GuavaCollectors() {}
    
}
