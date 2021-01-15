package com.ixaris.commons.emails.lib;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncExecutor.execAndRelay;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.executor.AsyncExecutorServiceWrapper;
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;
import com.ixaris.commons.async.lib.thread.NamedThreadFactory;
import com.ixaris.commons.emails.lib.config.CryptoConfig;
import com.ixaris.commons.emails.lib.config.SmtpConfig;
import com.ixaris.commons.emails.lib.security.BouncyCastleHelper;
import com.ixaris.commons.emails.lib.security.EmailSecurity;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * Holds implementation for email sending. Supports optional functionalities like email attachments and email content encryption.
 *
 * @author <a href="mailto:lazar.agatonovic@ixaris.com">lazar.agatonovic</a>
 */
public final class EmailFactory {
    
    public static void blockingSend(final String senderAddress,
                                    final String senderName,
                                    final String recipientAddress,
                                    final String recipientName,
                                    final String subject,
                                    final String textContent,
                                    final String htmlContent,
                                    final Set<File> attachments,
                                    final EmailSecurity emailSecurity,
                                    final SmtpConfig smtpConfig,
                                    final CryptoConfig cryptoConfig) {
        if (senderAddress == null) {
            throw new IllegalArgumentException("senderAddress is null");
        }
        if (recipientAddress == null) {
            throw new IllegalArgumentException("recipientAddress is null");
        }
        if (smtpConfig == null) {
            throw new IllegalArgumentException("smtpConfig is null");
        }
        if (!EmailSecurity.NONE.equals(emailSecurity) && cryptoConfig == null) {
            throw new IllegalArgumentException("cryptoConfig is null");
        }
        
        try {
            final Tuple2<Session, Transport> smtp = smtpConfig.getSMTPSession();
            
            // create a message
            MimeMessage msg = new MimeMessage(smtp.get1());
            
            // set the from, to and reply-to address
            final InternetAddress senderInternetAddress = new InternetAddress(senderAddress);
            if (senderName != null) {
                senderInternetAddress.setPersonal(senderName, "UTF-8");
            }
            msg.setFrom(senderInternetAddress);
            msg.setReplyTo(new Address[] { senderInternetAddress });
            final InternetAddress recipientInternetAddress = new InternetAddress(recipientAddress);
            if (recipientName != null) {
                recipientInternetAddress.setPersonal(recipientName, "UTF-8");
            }
            msg.setRecipient(Message.RecipientType.TO, recipientInternetAddress);
            
            // Setting the Subject and Content Type
            msg.setSubject((subject == null) ? "" : subject);
            assembleContent(msg, textContent, htmlContent, attachments);
            msg.saveChanges();
            
            // encrypt the message (if signed and encrypted)
            if (!EmailSecurity.NONE.equals(emailSecurity)) {
                msg = BouncyCastleHelper.signEncryptMsg(smtp.get1(), msg, senderAddress, recipientAddress, emailSecurity, cryptoConfig);
            }
            
            try (Transport transport = smtp.get2()) {
                transport.connect();
                transport.sendMessage(msg, msg.getAllRecipients());
            }
        } catch (final MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private static void assembleContent(final MimeMessage msg,
                                        final String textContent,
                                        final String htmlContent,
                                        final Set<File> attachments) throws MessagingException {
        final boolean hasText = (textContent != null) && !textContent.isEmpty();
        final boolean hasHtml = (htmlContent != null) && !htmlContent.isEmpty();
        final boolean hasAttachments = (attachments != null) && !attachments.isEmpty();
        
        if (!hasHtml && !hasAttachments) {
            msg.setText(hasText ? "" : textContent, "UTF-8");
        } else {
            final Multipart content = new MimeMultipart("alternative");
            
            // Part for plain text
            if (hasText) {
                final MimeBodyPart textContentPart = new MimeBodyPart();
                textContentPart.setContent(textContent, "text/plain; charset=UTF-8");
                content.addBodyPart(textContentPart);
            }
            if (hasHtml) {
                // Part for html
                final MimeBodyPart htmlContentPart = new MimeBodyPart();
                htmlContentPart.setContent(htmlContent, "text/html; charset=UTF-8");
                content.addBodyPart(htmlContentPart);
            }
            
            // Process attachments
            if (hasAttachments) {
                final Multipart attachmentsPart = new MimeMultipart();
                attachments.forEach(attachment -> {
                    try {
                        final MimeBodyPart mimeBodyPart = new MimeBodyPart();
                        final DataSource source = new FileDataSource(attachment);
                        mimeBodyPart.setDataHandler(new DataHandler(source));
                        mimeBodyPart.setFileName(attachment.getName());
                        attachmentsPart.addBodyPart(mimeBodyPart);
                    } catch (final MessagingException e) {
                        throw new IllegalStateException("Unable to add email attachments: ", e);
                    }
                });
                
                // if we have attachments... we place the msg content and attachments in separate BodyParts
                final MimeBodyPart bpContent = new MimeBodyPart();
                bpContent.setContent(content);
                final MimeBodyPart bpAttach = new MimeBodyPart();
                bpAttach.setContent(attachmentsPart);
                
                final Multipart globalBody = new MimeMultipart();
                globalBody.addBodyPart(bpContent);
                globalBody.addBodyPart(bpAttach);
                msg.setContent(globalBody);
            } else {
                msg.setContent(content);
            }
        }
    }
    
    private static final int CORE_THREAD_POOL_SIZE = 1;
    private static final int MAX_THREAD_POOL_SIZE = 10;
    
    private final SmtpConfig smtpConfig;
    private final CryptoConfig cryptoConfig;
    
    /**
     * The executor on which persistence tasks are executed.
     */
    private final ExecutorService executor;
    
    public EmailFactory(final SmtpConfig smtpConfig, final CryptoConfig cryptoConfig) {
        this.smtpConfig = smtpConfig;
        this.cryptoConfig = cryptoConfig;
        executor = new AsyncExecutorServiceWrapper<>(
            true, new ThreadPoolExecutor(
                CORE_THREAD_POOL_SIZE,
                MAX_THREAD_POOL_SIZE,
                2L,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("EmailFactory-")));
    }
    
    public void shutdown() {
        executor.shutdown();
    }
    
    public Async<Void> send(final String senderAddress,
                            final String recipientAddress,
                            final String subject,
                            final String textContent,
                            final String htmlContent,
                            final EmailSecurity emailSecurity) {
        return send(senderAddress, null, recipientAddress, null, subject, textContent, htmlContent, null, emailSecurity);
    }
    
    public Async<Void> send(final String senderAddress,
                            final String senderName,
                            final String recipientAddress,
                            final String recipientName,
                            final String subject,
                            final String textContent,
                            final String htmlContent,
                            final EmailSecurity emailSecurity) {
        return send(senderAddress,
            senderName,
            recipientAddress,
            recipientAddress,
            subject,
            textContent,
            htmlContent,
            null,
            emailSecurity);
    }
    
    public Async<Void> send(final String senderAddress,
                            final String senderName,
                            final String recipientAddress,
                            final String recipientName,
                            final String subject,
                            final String textContent,
                            final String htmlContent,
                            final Set<File> attachments,
                            final EmailSecurity emailSecurity) {
        return execAndRelay(executor, () -> {
            blockingSend(senderAddress,
                senderName,
                recipientAddress,
                recipientName,
                subject,
                textContent,
                htmlContent,
                attachments,
                emailSecurity,
                smtpConfig,
                cryptoConfig);
            return result();
        });
    }
    
}
