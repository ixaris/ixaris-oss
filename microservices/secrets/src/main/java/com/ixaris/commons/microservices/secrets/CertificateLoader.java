package com.ixaris.commons.microservices.secrets;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Retrieves client, server and private certificates for the current service and environment Created by tiago.cucki on 05/06/2017.
 */
public interface CertificateLoader {
    
    /**
     * @param certificateData
     * @return Certificate object from a certificate string
     */
    Certificate getCertificate(final String certificateData);
    
    /**
     * @return PrivateKey object from current environment and service, looking for the certificate file on secrets folder
     */
    PrivateKey getPrivateKey();
    
    /**
     * @return an encryption key derived from the private key
     */
    default byte[] getEncryptionKey() {
        return null; // NOSONAR
    }
    
}
