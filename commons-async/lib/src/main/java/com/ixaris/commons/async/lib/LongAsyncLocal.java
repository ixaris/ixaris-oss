package com.ixaris.commons.async.lib;

import com.ixaris.commons.async.lib.CommonsAsyncLib.AsyncLocalValue;

public final class LongAsyncLocal extends AsyncLocal<Long> {
    
    public LongAsyncLocal(final String key) {
        super(key);
    }
    
    public LongAsyncLocal(final String key, final boolean stackable) {
        super(key, stackable);
    }
    
    public LongAsyncLocal(final String key, final AsyncLocalValidatorTransformer<Long> validatorTransformer) {
        super(key, validatorTransformer);
    }
    
    @Override
    public AsyncLocalValue encode(final Long value) {
        return AsyncLocalValue.newBuilder().setLongValue(value).build();
    }
    
    @Override
    public Long decode(final AsyncLocalValue value) {
        return value.getLongValue();
    }
    
}
