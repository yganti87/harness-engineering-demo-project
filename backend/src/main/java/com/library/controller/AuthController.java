package com.library.controller;

import com.library.service.AuthService;
import com.library.types.dto.ApiResponse;
import com.library.types.dto.LoginRequest;
import com.library.types.dto.LoginResponse;
import com.library.types.dto.MessageResponse;
import com.library.types.dto.RegisterRequest;
import com.library.types.dto.ResendVerificationRequest;
import com.library.types.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 *
 * <p>Register creates a new user and sends a verification email.
 * Email must be verified before login is allowed.
 * Login returns a JWT token for verified users.
 * Client sends token in Authorization: Bearer &lt;token&gt; header.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth", description = "User registration and login")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Create a new user account")
    public ResponseEntity<ApiResponse<UserDto>> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        UserDto user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(user));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate and receive a JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Verify email", description = "Verify email via token from email link")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        String result = authService.verifyEmail(token);
        String html = buildVerificationHtml(result);
        return ResponseEntity.ok(html);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email",
        description = "Resend verification email (rate limited)")
    public ResponseEntity<ApiResponse<MessageResponse>> resendVerification(
        @Valid @RequestBody ResendVerificationRequest request
    ) {
        authService.resendVerification(request.getEmail());
        MessageResponse message = MessageResponse.builder()
            .message("If your email is registered and unverified, "
                + "a new verification email has been sent.")
            .build();
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    private String buildVerificationHtml(String result) {
        if ("success".equals(result)) {
            return "<!DOCTYPE html><html><body style='font-family:sans-serif;text-align:center;"
                + "padding:40px'>"
                + "<h1>Email Verified!</h1>"
                + "<p>Your email has been verified. You can now log in to the Library.</p>"
                + "<p><a href='http://localhost:8501'>Go to Library</a></p>"
                + "</body></html>";
        } else if ("expired".equals(result)) {
            return "<!DOCTYPE html><html><body style='font-family:sans-serif;text-align:center;"
                + "padding:40px'>"
                + "<h1>Link Expired</h1>"
                + "<p>Your verification link has expired. "
                + "Please request a new verification email.</p>"
                + "</body></html>";
        } else {
            return "<!DOCTYPE html><html><body style='font-family:sans-serif;text-align:center;"
                + "padding:40px'>"
                + "<h1>Invalid Link</h1>"
                + "<p>This verification link is invalid or has already been used.</p>"
                + "</body></html>";
        }
    }
}
