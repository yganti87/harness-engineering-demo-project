package com.library.service;

import com.library.types.dto.UserDto;
import java.util.UUID;

/**
 * Service for generating and validating JWT tokens.
 *
 * <p>Tokens contain userId and email. Client sends token in
 * Authorization: Bearer &lt;token&gt; header.
 */
public interface JwtService {

    /**
     * Generates a signed JWT for the given user.
     *
     * @param userId user ID
     * @param email  user email
     * @return signed JWT string
     */
    String generate(UUID userId, String email);

    /**
     * Validates the JWT and extracts user info.
     *
     * @param token the JWT string
     * @return UserDto with id and email, or null if invalid/expired
     */
    UserDto validate(String token);
}
