package com.ixaris.commons.dimensions.config.cache;

import com.ixaris.commons.dimensions.config.ConfigDef;

public interface ConfigCacheProvider {
    
    ConfigCache of(ConfigDef<?> def);
    
}
