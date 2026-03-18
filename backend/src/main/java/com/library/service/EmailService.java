package com.library.service;

/**
 * Service for sending transactional emails.
 */
public interface EmailService {

    /**
     * Sends an email verification link to the given address.
     *
     * @param toEmail the recipient email address
     * @param token   the verification token
     */
    void sendVerificationEmail(String toEmail, String token);
}
