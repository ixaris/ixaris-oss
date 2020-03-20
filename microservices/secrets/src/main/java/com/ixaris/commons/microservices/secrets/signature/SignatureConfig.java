package com.ixaris.commons.microservices.secrets.signature;

/**
 * Created by tiago.cucki on 11/07/2017.
 */
public final class SignatureConfig {
    
    private final String signatureAlgorithm;
    private final String modulePublicCertificate;
    
    public SignatureConfig(final String signatureAlgorithm, final String modulePublicCertificate) {
        this.signatureAlgorithm = signatureAlgorithm;
        this.modulePublicCertificate = modulePublicCertificate;
    }
    
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }
    
    public String getModulePublicCertificate() {
        return modulePublicCertificate;
    }
}
