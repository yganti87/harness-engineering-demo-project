package com.library.service;

/**
 * Exception thrown when a resend verification request is rate-limited.
 *
 * <p>Maps to HTTP 429 Too Many Requests.
 */
public class ResendRateLimitedException extends RuntimeException {

    public ResendRateLimitedException(String message) {
        super(message);
    }
}
