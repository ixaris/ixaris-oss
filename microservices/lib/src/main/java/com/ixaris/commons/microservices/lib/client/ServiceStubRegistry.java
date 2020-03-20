package com.ixaris.commons.microservices.lib.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ixaris.commons.microservices.lib.client.proxy.ServiceStubProxy;
import com.ixaris.commons.misc.lib.conversion.SnakeCaseHelper;

/**
 * <p>
 *     A simple registry of {@link ServiceStubProxy} instances in the platform.
 *     Each proxy is registered according to its bean name, which is structured by convention.
 * </p>
 * @author colin.cachia
 */
public class ServiceStubRegistry {
    
    private final Map<String, ServiceStubProxy> proxyMap = new HashMap<>();
    
    /**
     * Registers the proxy with the passed beanName as key
     * @param beanName the key referring to the passed proxy
     * @param proxy the proxy to register
     */
    public void register(final String beanName, final ServiceStubProxy proxy) {
        proxyMap.put(beanName, proxy);
    }
    
    /**
     * Fetches the registered proxy by service name
     * @param name the service name
     * @return an {@link Optional} containing the proxy if available, {@link Optional#empty()} otherwise
     */
    public Optional<ServiceStubProxy> fetchProxyByServiceName(final String name) {
        return Optional.ofNullable(proxyMap.get(generateBeanNameFromServiceName(name)));
    }
    
    private static String generateBeanNameFromServiceName(final String serviceName) {
        return SnakeCaseHelper.snakeToCamelCase(serviceName) + "Stub";
    }
}
