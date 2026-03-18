package com.library.types.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for login.
 *
 * <p>Contains user info and JWT token. Client sends token in
 * Authorization: Bearer &lt;token&gt; header on subsequent requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private UUID userId;
    private String email;
    private String token;
}
