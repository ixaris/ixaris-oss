package com.ixaris.commons.async.lib.logging;

import com.google.protobuf.InvalidProtocolBufferException;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.CommonsAsyncLib.AsyncLocalValue;
import com.ixaris.commons.async.lib.CommonsAsyncLib.LogContextValue;
import com.ixaris.commons.protobuf.lib.MessageHelper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AsyncLogging {
    
    public static final AsyncLocal<Map<String, String>> ASYNC_MDC = new AsyncLocal<Map<String, String>>("mdc", (
        value, prev
    ) -> {
        if (prev == null) {
            return Collections.unmodifiableMap(value);
        } else {
            final Map<String, String> map = new HashMap<>(prev);
            map.putAll(value); // overwrite with new values
            return Collections.unmodifiableMap(map);
        }
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
    
    private AsyncLogging() {}
    
}
