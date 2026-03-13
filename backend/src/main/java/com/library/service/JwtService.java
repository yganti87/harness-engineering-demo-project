package com.library.service;

import com.library.types.dto.UserDto;
import java.util.UUID;

/**
 * Service for generating and validating JWT tokens.
 *
 * <p>Tokens contain userId and username. Client sends token in
 * Authorization: Bearer &lt;token&gt; header.
 *
 * <p>See design-docs/002-session-strategy.md.
 */
public interface JwtService {

    /**
     * Generates a signed JWT for the given user.
     *
     * @param userId   user ID
     * @param username username
     * @return signed JWT string
     */
    String generate(UUID userId, String username);

    /**
     * Validates the JWT and extracts user info.
     *
     * @param token the JWT string
     * @return UserDto with id and username, or null if invalid/expired
     */
    UserDto validate(String token);
}
