package com.library.config;

import java.net.InetSocketAddress;
import java.net.Socket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator that checks SMTP connectivity.
 *
 * <p>Reports UP if the configured SMTP host:port is reachable, DOWN otherwise.
 * Visible at /actuator/health as "mail" component.
 */
@Component("mailSmtp")
@Slf4j
public class MailHealthIndicator implements HealthIndicator {

    private static final int CONNECTION_TIMEOUT_MS = 2000;

    private final String smtpHost;
    private final int smtpPort;

    public MailHealthIndicator(
        @Value("${spring.mail.host:localhost}") String smtpHost,
        @Value("${spring.mail.port:1025}") int smtpPort
    ) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
    }

    @Override
    public Health health() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(smtpHost, smtpPort), CONNECTION_TIMEOUT_MS);
            return Health.up()
                .withDetail("host", smtpHost)
                .withDetail("port", smtpPort)
                .build();
        } catch (Exception e) {
            log.warn("SMTP health check failed host={} port={} error={}", smtpHost, smtpPort,
                e.getMessage());
            return Health.down()
                .withDetail("host", smtpHost)
                .withDetail("port", smtpPort)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
