package com.ixaris.commons.multitenancy.spring;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for all multi-tenancy properties, that syncs with the configuration manager. Contains details such as connection strings for
 * different tenants, and their tenants.
 *
 * <p>The {@link ConfigurationProperties} annotation indicates what properties this class should load - the accessor tenants match the properties
 * at the configuration manager's side.
 *
 * @author benjie.gatt
 */
@ConfigurationProperties(prefix = "multitenancy")
@EnableConfigurationProperties
@Component
public class MultiTenancyTenants {
    
    private List<String> tenants;
    
    /**
     * @return the list of tenant tenants that are currently running in the system
     */
    public List<String> getTenants() {
        return tenants; // NOSONAR: Spring Config does not seem to work if we return immutable collections!
    }
    
    public void setTenants(final List<String> tenants) {
        this.tenants = tenants; // NOSONAR: Spring Config does not seem to work if we return immutable collections!
    }
    
}
