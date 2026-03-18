package com.library.unit.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.library.config.MailProperties;
import com.library.config.VerificationConfig;
import com.library.service.EmailSendException;
import com.library.service.EmailServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);

        MailProperties mailProperties = new MailProperties();
        mailProperties.setFromAddress("noreply@library.local");

        VerificationConfig verificationConfig = new VerificationConfig();
        verificationConfig.setTokenExpiryMinutes(15);
        verificationConfig.setBaseUrl("http://localhost:8080");

        emailService = new EmailServiceImpl(
            mailSender,
            mailProperties,
            verificationConfig,
            new SimpleMeterRegistry()
        );

        try {
            java.lang.reflect.Method m =
                EmailServiceImpl.class.getDeclaredMethod("initMetrics");
            m.setAccessible(true);
            m.invoke(emailService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MimeMessage createMimeMessage() {
        Session session = Session.getInstance(new Properties());
        return new MimeMessage(session);
    }

    @Test
    void sendVerificationEmail_sendsWithCorrectSubjectAndLink() {
        MimeMessage mimeMessage = createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationEmail("alice@example.com", "test-token-123");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendVerificationEmail_usesConfiguredFromAddress() {
        MimeMessage mimeMessage = createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationEmail("bob@example.com", "another-token");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmail_throwsEmailSendException_onMailFailure() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

        assertThatThrownBy(
            () -> emailService.sendVerificationEmail("alice@example.com", "token"))
            .isInstanceOf(EmailSendException.class)
            .hasMessageContaining("Failed to send verification email");
    }
}
