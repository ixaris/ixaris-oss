package com.ixaris.commons.logging.async;

import static com.ixaris.commons.async.lib.logging.AsyncLogging.ASYNC_MDC;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.CORRELATION;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.spi.CleanableThreadContextMap;
import org.apache.logging.log4j.spi.Helper;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.TriConsumer;

import com.google.common.collect.ImmutableMap;

import com.ixaris.commons.async.lib.CommonsAsyncLib.Correlation;

public final class AsyncContextMap implements CleanableThreadContextMap {
    
    public static final String KEY_TENANT_ID = "TENANT_ID";
    public static final String KEY_CORRELATION_ID = "CORRELATION_ID";
    public static final String KEY_INTENT_ID = "INTENT_ID";
    
    private static ImmutableMap<String, String> get() {
        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        final Map<String, String> mdc = ASYNC_MDC.get();
        if (mdc != null) {
            builder.putAll(mdc);
        }
        final Correlation correlation = CORRELATION.get();
        if (correlation != null) {
            builder
                .put(KEY_CORRELATION_ID, Long.toString(correlation.getCorrelationId()))
                .put(KEY_INTENT_ID, Long.toString(correlation.getIntentId()));
        }
        final String tenantId = TENANT.get();
        if (tenantId != null) {
            builder.put(KEY_TENANT_ID, tenantId);
        }
        return builder.build();
    }
    
    private static boolean getEmpty() {
        final Map<String, String> mdc = ASYNC_MDC.get();
        if ((mdc != null) && !mdc.isEmpty()) {
            return false;
        }
        if (CORRELATION.get() != null) {
            return false;
        }
        return TENANT.get() == null;
    }
    
    private CleanableThreadContextMap baseMap = Helper.create();
    
    @Override
    public void clear() {
        baseMap.clear();
    }
    
    @Override
    public boolean containsKey(final String key) {
        return get().containsKey(key) || baseMap.containsKey(key);
    }
    
    @Override
    public String get(final String key) {
        switch (key) {
            case KEY_CORRELATION_ID:
                return Long.toString(CORRELATION.get().getCorrelationId());
            case KEY_INTENT_ID:
                return Long.toString(CORRELATION.get().getIntentId());
            case KEY_TENANT_ID:
                return TENANT.get();
            default:
                return Optional.ofNullable(ASYNC_MDC.get()).map(m -> m.get(key)).orElseGet(() -> baseMap.get(key));
        }
    }
    
    @Override
    public Map<String, String> getCopy() {
        final Map<String, String> map = baseMap.getCopy();
        map.putAll(get());
        return map;
    }
    
    @Override
    public Map<String, String> getImmutableMapOrNull() {
        final Map<String, String> copy = getCopy();
        return copy.isEmpty() ? copy : Collections.unmodifiableMap(copy);
    }
    
    @Override
    public boolean isEmpty() {
        return getEmpty() && baseMap.isEmpty();
    }
    
    @Override
    public void put(final String key, final String value) {
        baseMap.put(key, value);
    }
    
    @Override
    public void putAll(final Map<String, String> map) {
        baseMap.putAll(map);
    }
    
    @Override
    public void remove(final String key) {
        baseMap.remove(key);
    }
    
    @Override
    public void removeAll(final Iterable<String> keys) {
        baseMap.removeAll(keys);
    }
    
    @Override
    @SuppressWarnings("squid:S1188")
    public StringMap getReadOnlyContextData() {
        final Map<String, String> copy = getCopy();
        return new StringMap() {
            
            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public void freeze() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public boolean isFrozen() {
                return true;
            }
            
            @Override
            public void putAll(final ReadOnlyStringMap source) {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public void putValue(final String key, final Object value) {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public void remove(final String key) {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Map<String, String> toMap() {
                return copy;
            }
            
            @Override
            public boolean containsKey(final String key) {
                return copy.containsKey(key);
            }
            
            @SuppressWarnings("unchecked")
            @Override
            public <V> void forEach(final BiConsumer<String, ? super V> action) {
                copy.forEach((k, v) -> action.accept(k, (V) v));
            }
            
            @SuppressWarnings("unchecked")
            @Override
            public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {
                copy.forEach((k, v) -> action.accept(k, (V) v, state));
            }
            
            @SuppressWarnings("unchecked")
            @Override
            public <V> V getValue(final String key) {
                return (V) copy.get(key);
            }
            
            @Override
            public boolean isEmpty() {
                return copy.isEmpty();
            }
            
            @Override
            public int size() {
                return copy.size();
            }
            
        };
    }
    
}
