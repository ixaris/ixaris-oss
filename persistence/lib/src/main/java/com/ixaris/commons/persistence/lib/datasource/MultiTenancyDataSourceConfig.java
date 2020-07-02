package com.ixaris.commons.persistence.lib.datasource;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "multitenancy")
@EnableConfigurationProperties
@Component
public class MultiTenancyDataSourceConfig {
    
    private Map<String, RelationalDbProperties> mysql;
    
    private MultiTenancyDataSourceDefaultsConfig defaults;
    
    public Map<String, RelationalDbProperties> getMysql() {
        return mysql;
    }
    
    public void setMysql(final Map<String, RelationalDbProperties> mysql) {
        this.mysql = mysql;
    }
    
    /**
     * @return the default properties to be used to connect to a database. To be used in case {@link #getMysql()} does not have an entry for this
     *     tenant.
     */
    public MultiTenancyDataSourceDefaultsConfig getDefaults() {
        return defaults;
    }
    
    public void setDefaults(final MultiTenancyDataSourceDefaultsConfig defaults) {
        this.defaults = defaults;
    }
    
}
