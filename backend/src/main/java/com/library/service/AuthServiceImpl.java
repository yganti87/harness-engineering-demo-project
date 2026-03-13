package com.library.service;

import com.library.repository.UserRepository;
import com.library.repository.entity.UserEntity;
import com.library.types.dto.LoginRequest;
import com.library.types.dto.LoginResponse;
import com.library.types.dto.RegisterRequest;
import com.library.types.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService}.
 *
 * <p>Passwords are hashed with BCrypt. Login returns a JWT token.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username already exists username='{}'",
                request.getUsername());
            throw new UsernameAlreadyExistsException(
                "Username already taken: " + request.getUsername());
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        UserEntity entity = UserEntity.builder()
            .username(request.getUsername())
            .passwordHash(passwordHash)
            .build();

        UserEntity saved = userRepository.save(entity);
        log.info("User registered userId='{}' username='{}'",
            saved.getId(), saved.getUsername());

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> {
                log.warn("Login failed: user not found username='{}'", request.getUsername());
                return new InvalidCredentialsException("Invalid username or password");
            });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password username='{}'", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtService.generate(user.getId(), user.getUsername());
        log.info("User logged in userId='{}' username='{}'",
            user.getId(), user.getUsername());

        return LoginResponse.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .token(token)
            .build();
    }

    private UserDto toDto(UserEntity entity) {
        return UserDto.builder()
            .id(entity.getId())
            .username(entity.getUsername())
            .build();
    }
}
