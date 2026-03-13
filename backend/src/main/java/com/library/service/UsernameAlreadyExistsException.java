package com.library.service;

/**
 * Thrown when registration is attempted with a username that already exists.
 *
 * <p>Maps to HTTP 409 Conflict.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
