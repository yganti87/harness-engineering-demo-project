package com.library.service;

import com.library.types.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link JwtService}.
 *
 * <p>Uses jjwt for HS256 signing. Tokens carry userId (subject) and email claim.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private static final String CLAIM_EMAIL = "email";

    private final com.library.config.JwtConfig jwtConfig;

    @Override
    public String generate(UUID userId, String email) {
        SecretKey key = Keys.hmacShaKeyFor(
            jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date expiry = new Date(now.getTime()
            + jwtConfig.getExpiryMinutes() * 60_000L);

        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_EMAIL, email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact();
    }

    @Override
    public UserDto validate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        SecretKey key = Keys.hmacShaKeyFor(
            jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));

        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);

            return UserDto.builder()
                .id(userId)
                .email(email)
                .build();
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired");
            return null;
        } catch (JwtException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return null;
        }
    }
}
