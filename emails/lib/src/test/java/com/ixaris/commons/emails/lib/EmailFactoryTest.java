package com.ixaris.commons.emails.lib;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.ixaris.commons.emails.lib.config.CryptoConfig;
import com.ixaris.commons.emails.lib.config.KeystoreCryptoConfig;
import com.ixaris.commons.emails.lib.config.SmtpConfig;
import com.ixaris.commons.emails.lib.security.EmailSecurity;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * Test class for {@link EmailFactory EmailFactory} To be implemented once receive email functionality is implemented.
 *
 * @author <a href="mailto:lazar.agatonovic@ixaris.com">lazar.agatonovic</a>
 */
public class EmailFactoryTest {
    
    private static final String SENDER_ADDRESS = "sender@mail.com";
    private static final String SENDER_NAME = "Sender";
    private static final String SENDER_NAME_ADDRESS = SENDER_NAME + " <" + SENDER_ADDRESS + ">";
    private static final String RECIPIENT_ADDRESS = "recipient@mail.com";
    private static final String SUBJECT = "Subject";
    private static final String TEXT_CONTENT = "text message content";
    private static final String HTML_CONTENT;
    
    // SMTP configuration values
    private static final String SMTP_HOST = "http://test.host.test";
    private static final String SMTP_USERNAME = "username";
    private static final String SMTP_PASSWORD = "password";
    private static final int SMTP_PORT = 25;
    
    static {
        HTML_CONTENT = generateHtmlContent();
    }
    
    private Transport transport;
    private SmtpConfig smtpConfig;
    
    @Before
    public void setup() {
        transport = Mockito.mock(Transport.class);
        smtpConfig = new SmtpConfig(false, "localhost", 9999, "u", "p") {
            
            @Override
            public Tuple2<Session, Transport> getSMTPSession() {
                return tuple(null, transport);
            }
            
        };
    }
    
    @Test
    public void send_plainEmail() throws Exception {
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            SUBJECT,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.NONE,
            smtpConfig,
            null /* encryptionConfig */
        );
        
        final ArgumentCaptor<MimeMessage> capturedMessage = ArgumentCaptor.forClass(MimeMessage.class);
        
        Mockito.verify(transport, Mockito.times(1)).sendMessage(capturedMessage.capture(), Mockito.any(Address[].class));
        
        assertEquals(SUBJECT, capturedMessage.getValue().getSubject());
        
        assertEquals(1, capturedMessage.getValue().getReplyTo().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getReplyTo()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getFrom().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getFrom()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getAllRecipients().length);
        assertEquals(RECIPIENT_ADDRESS, capturedMessage.getValue().getAllRecipients()[0].toString());
    }
    
    @Test
    public void send_signedEmail() throws Exception {
        final String subject = "Signed Subject";
        final CryptoConfig cryptoConfig = new KeystoreCryptoConfig(getClass().getResourceAsStream("/emails.p12"), "changeit");
        final ArgumentCaptor<MimeMessage> capturedMessage = ArgumentCaptor.forClass(MimeMessage.class);
        
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            subject,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.SIGN,
            smtpConfig,
            cryptoConfig);
        
        Mockito.verify(transport, Mockito.times(1)).sendMessage(capturedMessage.capture(), Mockito.any(Address[].class));
        
        assertEquals(subject, capturedMessage.getValue().getSubject());
        
        assertEquals(1, capturedMessage.getValue().getReplyTo().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getReplyTo()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getFrom().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getFrom()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getAllRecipients().length);
        assertEquals(RECIPIENT_ADDRESS, capturedMessage.getValue().getAllRecipients()[0].toString());
    }
    
    @Test
    public void send_encryptedEmail() throws Exception {
        final String subject = "Encrypted Subject";
        final CryptoConfig cryptoConfig = new KeystoreCryptoConfig(getClass().getResourceAsStream("/emails.p12"), "changeit");
        final ArgumentCaptor<MimeMessage> capturedMessage = ArgumentCaptor.forClass(MimeMessage.class);
        
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            subject,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.ENCRYPT,
            smtpConfig,
            cryptoConfig);
        
        Mockito.verify(transport, Mockito.times(1)).sendMessage(capturedMessage.capture(), Mockito.any(Address[].class));
        
        assertEquals(subject, capturedMessage.getValue().getSubject());
        
        assertEquals(1, capturedMessage.getValue().getReplyTo().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getReplyTo()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getFrom().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getFrom()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getAllRecipients().length);
        assertEquals(RECIPIENT_ADDRESS, capturedMessage.getValue().getAllRecipients()[0].toString());
    }
    
    @Test
    public void send_signedEncryptedEmail() throws Exception {
        final String subject = "Signed Encrypted Subject";
        final CryptoConfig cryptoConfig = new KeystoreCryptoConfig(getClass().getResourceAsStream("/emails.p12"), "changeit");
        final ArgumentCaptor<MimeMessage> capturedMessage = ArgumentCaptor.forClass(MimeMessage.class);
        
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            subject,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.SIGN_ENCRYPT,
            smtpConfig,
            cryptoConfig);
        
        Mockito.verify(transport, Mockito.times(1)).sendMessage(capturedMessage.capture(), Mockito.any(Address[].class));
        
        assertEquals(subject, capturedMessage.getValue().getSubject());
        
        assertEquals(1, capturedMessage.getValue().getReplyTo().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getReplyTo()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getFrom().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getFrom()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getAllRecipients().length);
        assertEquals(RECIPIENT_ADDRESS, capturedMessage.getValue().getAllRecipients()[0].toString());
    }
    
    @Test
    public void send_plainEmailWithAttachments() throws Exception {
        final Set<File> attachments = new HashSet<>();
        File attachment = Mockito.mock(File.class);
        Mockito.when(attachment.getName()).thenReturn("fileName");
        attachments.add(attachment);
        
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            SUBJECT,
            TEXT_CONTENT,
            HTML_CONTENT,
            attachments,
            EmailSecurity.NONE,
            smtpConfig,
            null);
        
        final ArgumentCaptor<MimeMessage> capturedMessage = ArgumentCaptor.forClass(MimeMessage.class);
        
        Mockito.verify(transport, Mockito.times(1)).sendMessage(capturedMessage.capture(), Mockito.any(Address[].class));
        
        assertEquals(SUBJECT, capturedMessage.getValue().getSubject());
        
        assertEquals(1, capturedMessage.getValue().getReplyTo().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getReplyTo()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getFrom().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getFrom()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getAllRecipients().length);
        assertEquals(RECIPIENT_ADDRESS, capturedMessage.getValue().getAllRecipients()[0].toString());
    }
    
    @Test
    public void send_plainEmailWithoutHTMLContent() throws Exception {
        final ArgumentCaptor<MimeMessage> capturedMessage = ArgumentCaptor.forClass(MimeMessage.class);
        
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            SUBJECT,
            TEXT_CONTENT,
            null /* HTML_CONTENT */,
            null,
            EmailSecurity.NONE,
            smtpConfig,
            null);
        
        Mockito.verify(transport, Mockito.times(1)).sendMessage(capturedMessage.capture(), Mockito.any(Address[].class));
        
        assertEquals(SUBJECT, capturedMessage.getValue().getSubject());
        
        assertEquals(1, capturedMessage.getValue().getReplyTo().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getReplyTo()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getFrom().length);
        assertEquals(SENDER_NAME_ADDRESS, capturedMessage.getValue().getFrom()[0].toString());
        
        assertEquals(1, capturedMessage.getValue().getAllRecipients().length);
        assertEquals(RECIPIENT_ADDRESS, capturedMessage.getValue().getAllRecipients()[0].toString());
    }
    
    @Test
    public void send_plainEmailWithTextAndHTMLContent() throws Exception {
        final ArgumentCaptor<MimeMessage> capturedMessage = ArgumentCaptor.forClass(MimeMessage.class);
        
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            SUBJECT,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.NONE,
            smtpConfig,
            null);
        
        Mockito.verify(transport, Mockito.times(1)).sendMessage(capturedMessage.capture(), Mockito.any(Address[].class));
        
        assertThat(capturedMessage.getValue().getContent()).isInstanceOf(Multipart.class);
        final Multipart multipartContent = (Multipart) capturedMessage.getValue().getContent();
        assertThat(multipartContent.getCount()).isEqualTo(2);
        assertThat(multipartContent.getBodyPart(0).getContent()).isEqualTo(TEXT_CONTENT);
        assertThat(multipartContent.getBodyPart(1).getContent()).isEqualTo(HTML_CONTENT);
    }
    
    @Test
    public void send_plainEmailWithHTMLContentOnly() throws Exception {
        final ArgumentCaptor<MimeMessage> capturedMessage = ArgumentCaptor.forClass(MimeMessage.class);
        
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            SUBJECT,
            null,
            HTML_CONTENT,
            null,
            EmailSecurity.NONE,
            smtpConfig,
            null);
        
        Mockito.verify(transport, Mockito.times(1)).sendMessage(capturedMessage.capture(), Mockito.any(Address[].class));
        
        assertThat(capturedMessage.getValue().getContent()).isInstanceOf(Multipart.class);
        final Multipart multipartContent = (Multipart) capturedMessage.getValue().getContent();
        assertThat(multipartContent.getCount()).isEqualTo(1);
        assertThat(multipartContent.getBodyPart(0).getContent()).isEqualTo(HTML_CONTENT);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void send_PlainEmailWithoutSenderAddress() {
        EmailFactory.blockingSend(null /* SENDER_ADDRESS */,
            null,
            RECIPIENT_ADDRESS,
            null,
            SUBJECT,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.NONE,
            smtpConfig,
            null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void send_PlainEmailWithoutRecipientAddress() {
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            null /* RECIPIENT_ADDRESS */,
            null,
            SUBJECT,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.NONE,
            smtpConfig,
            null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void send_PlainEmailWithoutSMTPConfiguration() {
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            SUBJECT,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.NONE,
            null /* smtpConfig */,
            null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void send_EncryptedEmailWithoutEncryptionConfiguration() {
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            SUBJECT,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.ENCRYPT,
            smtpConfig,
            null);
    }
    
    @Test(expected = IllegalStateException.class)
    public void call_getSMTPSessionNonSecured() {
        final SmtpConfig smtpConfig = new SmtpConfig(false, SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD);
        
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            SUBJECT,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.NONE,
            smtpConfig,
            Mockito.mock(CryptoConfig.class));
    }
    
    @Test(expected = IllegalStateException.class)
    public void call_getSMTPSessionSecured() {
        final SmtpConfig smtpConfig = new SmtpConfig(true, SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD);
        
        EmailFactory.blockingSend(SENDER_ADDRESS,
            SENDER_NAME,
            RECIPIENT_ADDRESS,
            null,
            SUBJECT,
            TEXT_CONTENT,
            HTML_CONTENT,
            null,
            EmailSecurity.NONE,
            smtpConfig,
            Mockito.mock(CryptoConfig.class));
    }
    
    private static String generateHtmlContent() {
        return "<html>\n<body>\n<p> This is the paragraph</p>\n</body>\n</html>\n";
    }
    
}
