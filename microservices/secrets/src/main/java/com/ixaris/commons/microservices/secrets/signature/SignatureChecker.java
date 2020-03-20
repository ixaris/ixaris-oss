package com.ixaris.commons.microservices.secrets.signature;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLiteOrBuilder;

import com.ixaris.commons.microservices.secrets.CertificateLoader;
import com.ixaris.commons.misc.lib.conversion.Base64Util;
import com.ixaris.commons.protobuf.lib.MessageHelper;

/**
 * Provides signature verification for given MessageLite object Created by tiago.cucki on 04/07/2017.
 */
public class SignatureChecker {
    
    private static final Logger LOG = LoggerFactory.getLogger(SignatureChecker.class);
    
    private final Certificate publicCertificate;
    private final SignatureConfig signatureConfig;
    
    public SignatureChecker(final CertificateLoader certificateLoader, final SignatureConfig signatureConfig) {
        this.signatureConfig = signatureConfig;
        publicCertificate = certificateLoader.getCertificate(signatureConfig.getModulePublicCertificate());
    }
    
    /**
     * Verifies whether the signature is valid for the given object.
     *
     * <p>All object's attributes must be the same value as when the signature was generated (if the object has a <b>signature</b> attribute, it
     * must be cleaned before checked).
     *
     * @param object The object to have it's signature checked. All attributes must have the same value as when the signature was generated.
     * @param signature The signature to be verified
     * @return <code>true</code> in case the signature is valid.
     */
    public boolean checkSignature(final MessageLiteOrBuilder object, final String signature) {
        if (object == null) {
            throw new IllegalArgumentException("object is null");
        }
        if (signature == null) {
            throw new IllegalArgumentException("signature is null");
        }
        
        try {
            final Signature rsa = Signature.getInstance(signatureConfig.getSignatureAlgorithm());
            rsa.initVerify(publicCertificate.getPublicKey());
            rsa.update(Long.toString(MessageHelper.fingerprint(object)).getBytes(StandardCharsets.UTF_8));
            return rsa.verify(Base64Util.decode(signature));
        } catch (final Exception e) {
            LOG.error("An error occurred while signing a subject", e);
            return false;
        }
    }
}
