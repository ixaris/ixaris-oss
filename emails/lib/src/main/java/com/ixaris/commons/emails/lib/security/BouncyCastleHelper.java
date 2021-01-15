package com.ixaris.commons.emails.lib.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.Strings;

import com.ixaris.commons.emails.lib.config.CryptoConfig;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * Contains email encryption/signing logic using BouncyCastle provider.
 *
 * @author <a href="mailto:lazar.agatonovic@ixaris.com">lazar.agatonovic</a>
 */
public final class BouncyCastleHelper {
    
    static {
        final MailcapCommandMap mailcap = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mailcap
            .addMailcap("application/pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_signature");
        mailcap.addMailcap("application/pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_mime");
        mailcap
            .addMailcap("application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_signature");
        mailcap.addMailcap("application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_mime");
        mailcap.addMailcap("multipart/signed;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.multipart_signed");
        
        CommandMap.setDefaultCommandMap(mailcap);
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * Signs and encrypts an email message using the method specified with EmailSecurity parameter
     *
     * @param session email session
     * @param message email content
     * @param emailSecurity email security type
     * @param cryptoConfig encryption configuration
     * @return MIME message
     */
    public static MimeMessage signEncryptMsg(final Session session,
                                             final MimeMessage message,
                                             final String senderAlias,
                                             final String recipientAlias,
                                             final EmailSecurity emailSecurity,
                                             final CryptoConfig cryptoConfig) {
        MimeMessage m = message;
        if (emailSecurity.isSign()) {
            final Tuple2<PrivateKey, Certificate[]> sender = cryptoConfig.getSenderPrivateKeyAndCertificateChain(
                senderAlias);
            m = signMessage(session, m, sender.get1(), sender.get2());
        }
        if (emailSecurity.isEncrypt()) {
            m = encryptMessage(session, m, cryptoConfig.getRecipientCertificateChain(recipientAlias));
        }
        
        try {
            m.saveChanges();
        } catch (final MessagingException e) {
            throw new IllegalStateException(e);
        }
        return m;
    }
    
    private static MimeMessage signMessage(final Session session,
                                           final MimeMessage m,
                                           final PrivateKey senderKey,
                                           final Certificate[] senderChain) {
        
        try {
            final SMIMECapabilityVector capabilities = new SMIMECapabilityVector();
            capabilities.addCapability(SMIMECapability.dES_EDE3_CBC);
            capabilities.addCapability(SMIMECapability.rC2_CBC, 128);
            capabilities.addCapability(SMIMECapability.dES_CBC);
            
            final X509Certificate senderChain0 = (X509Certificate) senderChain[0];
            final ASN1EncodableVector attributes = new ASN1EncodableVector();
            attributes.add(new SMIMEEncryptionKeyPreferenceAttribute(new IssuerAndSerialNumber(new X500Name(senderChain0.getIssuerDN().getName()),
                senderChain0.getSerialNumber())));
            attributes.add(new SMIMECapabilitiesAttribute(capabilities));
            final AttributeTable attributeTable = new AttributeTable(attributes);
            final String algorithm = "DSA".equals(senderKey.getAlgorithm()) ? "SHA1withDSA" : "SHA1withRSA";
            
            final SMIMESignedGenerator signer = new SMIMESignedGenerator();
            signer.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder()
                .setProvider("BC")
                .setSignedAttributeGenerator(attributeTable)
                .build(algorithm, senderKey, senderChain0));
            
            /* Add the list of certs to the generator */
            final List<Certificate> certList = new ArrayList<>();
            certList.add(senderChain[0]);
            final Store certs = new JcaCertStore(certList);
            signer.addCertificates(certs);
            
            final MimeMessage signedMessage = new MimeMessage(session);
            // Set all original MIME headers in the signed message
            final Enumeration<?> headers = m.getAllHeaderLines();
            while (headers.hasMoreElements()) {
                signedMessage.addHeaderLine((String) headers.nextElement());
            }
            
            /* Set the content of the signed message */
            signedMessage.setContent(signer.generate(m, "BC"));
            signedMessage.saveChanges();
            
            return signedMessage;
            
        } catch (final SMIMEException e) {
            throw new IllegalStateException(e.getUnderlyingException());
        } catch (final GeneralSecurityException | OperatorException | MessagingException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private static MimeMessage encryptMessage(final Session session, final MimeMessage m, final Certificate[] recipientChain) {
        try {
            final SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
            encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator((X509Certificate) recipientChain[0]).setProvider("BC"));
            
            /* Encrypt the message */
            final MimeBodyPart encryptedPart = encrypter.generate(m,
                new JceCMSContentEncryptorBuilder(CMSAlgorithm.RC2_CBC).setProvider("BC").build());
            
            // Create a new MimeMessage that contains the encrypted and signed content
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            encryptedPart.writeTo(out);
            
            final MimeMessage encryptedMessage = new MimeMessage(session, new ByteArrayInputStream(out.toByteArray()));
            
            // Set all original MIME headers in the encrypted message
            final Enumeration<?> headers = m.getAllHeaderLines();
            while (headers.hasMoreElements()) {
                String headerLine = (String) headers.nextElement();
                // Make sure not to override any content-* headers from the original message
                if (!Strings.toLowerCase(headerLine).startsWith("content-")) {
                    encryptedMessage.addHeaderLine(headerLine);
                }
            }
            
            return encryptedMessage;
        } catch (final SMIMEException e) {
            throw new IllegalStateException(e.getUnderlyingException());
        } catch (final CertificateException | OperatorException | CMSException | MessagingException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private BouncyCastleHelper() {}
    
}
