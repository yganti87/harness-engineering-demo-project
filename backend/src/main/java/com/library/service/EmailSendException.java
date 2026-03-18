package com.library.service;

/**
 * Exception thrown when a transactional email fails to send.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
