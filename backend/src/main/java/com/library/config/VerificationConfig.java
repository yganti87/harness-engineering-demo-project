package com.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for email verification.
 *
 * <p>Bound from {@code app.verification.*} in application.yml.
 */
@ConfigurationProperties(prefix = "app.verification")
public class VerificationConfig {

    private int tokenExpiryMinutes = 15;
    private int resendCooldownSeconds = 60;
    private String baseUrl = "http://localhost:8080";

    public int getTokenExpiryMinutes() {
        return tokenExpiryMinutes;
    }

    public void setTokenExpiryMinutes(int tokenExpiryMinutes) {
        this.tokenExpiryMinutes = tokenExpiryMinutes;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
