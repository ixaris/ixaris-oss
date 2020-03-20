package com.ixaris.commons.async.lib;

import com.ixaris.commons.async.lib.CommonsAsyncLib.AsyncLocalValue;

public final class StringAsyncLocal extends AsyncLocal<String> {
    
    public StringAsyncLocal(final String key) {
        super(key);
    }
    
    public StringAsyncLocal(final String key, final boolean stackable) {
        super(key, stackable);
    }
    
    public StringAsyncLocal(final String key, final AsyncLocalValidatorTransformer<String> validatorTransformer) {
        super(key, validatorTransformer);
    }
    
    @Override
    public AsyncLocalValue encode(final String value) {
        return AsyncLocalValue.newBuilder().setStringValue(value).build();
    }
    
    @Override
    public String decode(final AsyncLocalValue value) {
        return value.getStringValue();
    }
    
}
