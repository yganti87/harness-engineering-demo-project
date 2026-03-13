package com.library.service;

/**
 * Thrown when login fails due to invalid username or password.
 *
 * <p>Maps to HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
