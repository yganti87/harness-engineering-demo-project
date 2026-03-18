package com.library.service;

/**
 * Exception thrown when a user attempts to log in before verifying their email.
 *
 * <p>Maps to HTTP 403 Forbidden.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
