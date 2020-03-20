package com.ixaris.commons.microservices.secrets;

/**
 * Created by tiago.cucki on 05/06/2017.
 */
@Deprecated
public final class CertificateConfig {
    
    private final String environment;
    private final String serviceName;
    private final String certificateRootPath;
    
    public CertificateConfig(final String environment, final String serviceName, final String certificateRootPath) {
        this.environment = environment;
        this.serviceName = serviceName;
        this.certificateRootPath = certificateRootPath;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getCertificateRootPath() {
        return certificateRootPath;
    }
}
