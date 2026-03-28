package com.library.service;

import com.library.config.MailProperties;
import com.library.config.VerificationConfig;
import com.library.types.util.EmailMaskUtil;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link EmailService}.
 *
 * <p>Sends HTML verification emails via JavaMailSender. Logs outcomes and
 * increments Prometheus counters.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final VerificationConfig verificationConfig;
    private final MeterRegistry meterRegistry;

    @Override
    @Timed(value = "email.send_verification")
    public void sendVerificationEmail(String toEmail, String token) {
        String verificationUrl = verificationConfig.getBaseUrl()
            + "/api/v1/auth/verify?token=" + token;
        String maskedEmail = EmailMaskUtil.mask(toEmail);

        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailProperties.getFromAddress());
            helper.setTo(toEmail);
            helper.setSubject("Verify your Library account");
            helper.setText(buildEmailBody(verificationUrl), true);
            mailSender.send(message);
            meterRegistry.counter("auth_verification_email_sent_total", "status", "success")
                .increment();
            log.info("Verification email sent email='{}' tokenExpiresAt='{} minutes'",
                maskedEmail, verificationConfig.getTokenExpiryMinutes());
        } catch (Exception e) {
            meterRegistry.counter("auth_verification_email_sent_total", "status", "error")
                .increment();
            log.error("Failed to send verification email email='{}' error='{}'",
                maskedEmail, e.getMessage());
            throw new EmailSendException("Failed to send verification email to " + maskedEmail, e);
        }
    }

    private String buildEmailBody(String verificationUrl) {
        return "<html><body>"
            + "<h2>Verify your email address</h2>"
            + "<p>Click the link below to verify your email and activate your account.</p>"
            + "<p><a href=\"" + verificationUrl + "\">Verify Email</a></p>"
            + "<p>This link expires in " + verificationConfig.getTokenExpiryMinutes()
            + " minutes.</p>"
            + "<p>If you did not create an account, you can ignore this email.</p>"
            + "</body></html>";
    }
}
