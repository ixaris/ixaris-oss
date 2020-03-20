package com.ixaris.commons.async.lib.logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.CommonsAsyncLib.AsyncLocalValue;
import com.ixaris.commons.async.lib.CommonsAsyncLib.Correlation;
import com.ixaris.commons.async.lib.CommonsAsyncLib.LogContextValue;
import com.ixaris.commons.misc.lib.startup.StartupTask;
import com.ixaris.commons.protobuf.lib.MessageHelper;

public class AsyncLogging implements StartupTask {
    
    public static final AsyncLocal<Correlation> CORRELATION = new AsyncLocal<Correlation>("correlation") {
        
        @Override
        public AsyncLocalValue encode(final Correlation value) {
            return AsyncLocalValue.newBuilder().setBytesValue(value.toByteString()).build();
        }
        
        @Override
        public Correlation decode(final AsyncLocalValue value) throws InvalidProtocolBufferException {
            return MessageHelper.parse(Correlation.class, value.getBytesValue());
        }
        
    };
    
    public static final AsyncLocal<Map<String, String>> ASYNC_MDC = new AsyncLocal<Map<String, String>>("mdc", (value, prev) -> {
        final Map<String, String> map;
        if (prev == null) {
            map = value;
        } else {
            map = new HashMap<>(prev);
            map.putAll(value); // overwrite with new values
        }
        return Collections.unmodifiableMap(map);
    }) {
        
        @Override
        public AsyncLocalValue encode(final Map<String, String> value) {
            return AsyncLocalValue.newBuilder()
                .setBytesValue(LogContextValue.newBuilder().putAllContext(value).build().toByteString())
                .build();
        }
        
        @Override
        public Map<String, String> decode(final AsyncLocalValue value) throws InvalidProtocolBufferException {
            return MessageHelper.parse(LogContextValue.class, value.getBytesValue()).getContextMap();
        }
        
    };
    
    @Override
    public void run() {
        // force register async locals    
    }
    
}
