package com.library.controller;

import com.library.service.AuthService;
import com.library.types.dto.ApiResponse;
import com.library.types.dto.LoginRequest;
import com.library.types.dto.LoginResponse;
import com.library.types.dto.RegisterRequest;
import com.library.types.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 *
 * <p>Register creates a new user. Login returns a JWT token.
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
}
