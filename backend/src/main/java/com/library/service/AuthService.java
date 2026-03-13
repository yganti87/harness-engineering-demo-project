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
     * Registers a new user.
     *
     * @param request registration request
     * @return the created user (without password)
     * @throws UsernameAlreadyExistsException if username already taken
     */
    UserDto register(RegisterRequest request);

    /**
     * Authenticates a user and returns a JWT.
     *
     * @param request login request
     * @return login response with user info and token
     * @throws InvalidCredentialsException if username or password invalid
     */
    LoginResponse login(LoginRequest request);
}
