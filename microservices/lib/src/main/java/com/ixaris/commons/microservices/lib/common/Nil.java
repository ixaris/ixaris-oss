package com.ixaris.commons.microservices.lib.common;

import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;

public class Nil {
    
    private static final Nil INSTANCE = new Nil();
    
    private static final Async<Nil> ASYNC_INSTANCE = result(INSTANCE);
    
    public static Nil getInstance() {
        return INSTANCE;
    }
    
    public static Async<Nil> getAsyncInstance() {
        return ASYNC_INSTANCE;
    }
    
    private Nil() {}
    
}
