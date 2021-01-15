package com.ixaris.commons.dimensions.lib.base;

import com.ixaris.commons.dimensions.lib.context.Context;

public abstract class AbstractContextEntity<D extends DimensionalDef> {
    
    private final long id;
    private final D def;
    private final Context<D> context;
    
    public AbstractContextEntity(final long id, final D def, final Context<D> context) {
        this.id = id;
        this.def = def;
        this.context = context;
    }
    
    public long getId() {
        return id;
    }
    
    public D getDef() {
        return def;
    }
    
    public Context<D> getContext() {
        return context;
    }
    
}
