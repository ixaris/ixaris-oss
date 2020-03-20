package com.ixaris.commons.multitenancy.lib.data;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.StringAsyncLocal;

public interface DataUnit {
    
    /**
     * DATA_UNIT represents a unit of data, typically a service, but can be finer grained
     */
    AsyncLocal<String> DATA_UNIT = new StringAsyncLocal("data_unit");
    
    String get();
    
    default String getUnit() {
        return get();
    }
    
}
