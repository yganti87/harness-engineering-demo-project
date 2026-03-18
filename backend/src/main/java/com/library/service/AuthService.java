package com.library.service;

import com.library.types.dto.LoginRequest;
import com.library.types.dto.LoginResponse;
import com.library.types.dto.RegisterRequest;
import com.library.types.dto.UserDto;

/**
 * Service for user registration and authentication.
 */
public interface AuthService {

    /**
     * Registers a new user and sends a verification email.
     *
     * @param request registration request (email, password, confirmPassword)
     * @return the created user (without password, emailVerified=false)
     * @throws EmailAlreadyExistsException if the email is already registered
     */
    UserDto register(RegisterRequest request);

    /**
     * Authenticates a verified user and returns a JWT.
     *
     * @param request login request (email, password)
     * @return login response with user info and token
     * @throws InvalidCredentialsException  if email or password is invalid
     * @throws EmailNotVerifiedException    if email has not been verified yet
     */
    LoginResponse login(LoginRequest request);

    /**
     * Verifies an email via a verification token.
     *
     * @param token the verification token from the email link
     * @return "success" if verified, "expired" if token expired, "invalid" if token not found
     */
    String verifyEmail(String token);

    /**
     * Resends a verification email to the given address (rate limited).
     *
     * <p>Silently ignores unknown or already-verified addresses to prevent enumeration.
     *
     * @param email the email address to resend to
     */
    void resendVerification(String email);
}
