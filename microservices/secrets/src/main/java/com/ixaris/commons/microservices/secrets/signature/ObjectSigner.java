package com.ixaris.commons.microservices.secrets.signature;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;

import com.google.protobuf.MessageLiteOrBuilder;

import com.ixaris.commons.microservices.secrets.CertificateLoader;
import com.ixaris.commons.misc.lib.conversion.Base64Util;
import com.ixaris.commons.protobuf.lib.MessageHelper;

/**
 * Provides a signature for a MessageLite object (by its fingerprint) Created by tiago.cucki on 04/07/2017.
 */
public final class ObjectSigner {
    
    private final PrivateKey privateKey;
    
    private final SignatureConfig signatureConfig;
    
    public ObjectSigner(final CertificateLoader certificateLoader, final SignatureConfig signatureConfig) {
        this.signatureConfig = signatureConfig;
        privateKey = certificateLoader.getPrivateKey();
    }
    
    public String getObjectSignature(final MessageLiteOrBuilder object) {
        if (object == null) {
            throw new IllegalArgumentException("object is null");
        }
        
        try {
            final Signature rsa = Signature.getInstance(signatureConfig.getSignatureAlgorithm());
            rsa.initSign(privateKey);
            rsa.update(Long.toString(MessageHelper.fingerprint(object)).getBytes(StandardCharsets.UTF_8));
            final byte[] signature = rsa.sign();
            return Base64Util.encode(signature);
        } catch (Exception e) {
            throw new IllegalStateException("Error on signing a subject", e);
        }
    }
    
}
