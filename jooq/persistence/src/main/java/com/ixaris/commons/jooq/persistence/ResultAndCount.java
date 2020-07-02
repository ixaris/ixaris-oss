package com.ixaris.commons.jooq.persistence;

import java.util.List;

public final class ResultAndCount<R> {
    
    private final List<R> result;
    private final int count;
    
    public ResultAndCount(final List<R> result, final int count) {
        this.result = result;
        this.count = count;
    }
    
    public ResultAndCount(final List<R> result) {
        this(result, 0);
    }
    
    public List<R> getResult() {
        return result;
    }
    
    public int getCount() {
        return count;
    }
    
}
