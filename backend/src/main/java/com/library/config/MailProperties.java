package com.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for mail sending.
 *
 * <p>Bound from {@code app.mail.*} in application.yml.
 */
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private String fromAddress = "noreply@library.local";

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
}
