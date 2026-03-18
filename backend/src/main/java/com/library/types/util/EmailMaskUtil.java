package com.library.types.util;

/**
 * Utility for masking email addresses in log messages.
 *
 * <p>Never log full email addresses. Use this utility to produce
 * masked versions: first char + "***" + "@" + domain.
 * Example: "alice@example.com" → "a***@example.com"
 */
public final class EmailMaskUtil {

    private EmailMaskUtil() {
        // Utility class — no instances
    }

    /**
     * Masks an email address for safe logging.
     *
     * <p>Returns the first character of the local part, followed by "***",
     * then "@" and the domain. If the email is null, empty, or malformed,
     * returns "[invalid-email]".
     *
     * @param email the email address to mask
     * @return masked email string
     */
    public static String mask(String email) {
        if (email == null || email.isBlank()) {
            return "[invalid-email]";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "[invalid-email]";
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return localPart.charAt(0) + "***" + domain;
    }
}
