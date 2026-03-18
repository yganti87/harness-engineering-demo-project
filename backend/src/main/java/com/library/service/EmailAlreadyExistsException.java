package com.library.service;

/**
 * Exception thrown when a user attempts to register with an email that is already in use.
 *
 * <p>Maps to HTTP 409 Conflict.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
