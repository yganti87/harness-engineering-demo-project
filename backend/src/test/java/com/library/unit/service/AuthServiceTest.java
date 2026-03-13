package com.library.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.library.repository.UserRepository;
import com.library.repository.entity.UserEntity;
import com.library.service.AuthServiceImpl;
import com.library.service.InvalidCredentialsException;
import com.library.service.JwtService;
import com.library.service.UsernameAlreadyExistsException;
import com.library.types.dto.LoginRequest;
import com.library.types.dto.LoginResponse;
import com.library.types.dto.RegisterRequest;
import com.library.types.dto.UserDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_validRequest_returnsUserDto() {
        RegisterRequest request = RegisterRequest.builder()
            .username("alice")
            .password("secret123")
            .confirmPassword("secret123")
            .build();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(UserEntity.class)))
            .thenAnswer(inv -> {
                UserEntity e = inv.getArgument(0);
                return UserEntity.builder()
                    .id(UUID.randomUUID())
                    .username(e.getUsername())
                    .passwordHash(e.getPasswordHash())
                    .build();
            });

        UserDto result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void register_duplicateUsername_throwsUsernameAlreadyExistsException() {
        RegisterRequest request = RegisterRequest.builder()
            .username("alice")
            .password("secret123")
            .confirmPassword("secret123")
            .build();

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(UsernameAlreadyExistsException.class)
            .hasMessageContaining("alice");
    }

    @Test
    void login_validCredentials_returnsLoginResponse() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
            .id(userId)
            .username("alice")
            .passwordHash("$2a$12$hashed")
            .build();

        LoginRequest request = LoginRequest.builder()
            .username("alice")
            .password("secret123")
            .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "$2a$12$hashed")).thenReturn(true);
        when(jwtService.generate(eq(userId), eq("alice"))).thenReturn("jwt-token-123");

        LoginResponse result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getToken()).isEqualTo("jwt-token-123");
    }

    @Test
    void login_invalidUsername_throwsInvalidCredentialsException() {
        LoginRequest request = LoginRequest.builder()
            .username("nonexistent")
            .password("secret123")
            .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_invalidPassword_throwsInvalidCredentialsException() {
        UserEntity user = UserEntity.builder()
            .id(UUID.randomUUID())
            .username("alice")
            .passwordHash("$2a$12$hashed")
            .build();

        LoginRequest request = LoginRequest.builder()
            .username("alice")
            .password("wrongpassword")
            .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$12$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessageContaining("Invalid username or password");
    }
}
