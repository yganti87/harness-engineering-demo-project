package com.library.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.library.config.VerificationConfig;
import com.library.repository.EmailVerificationTokenRepository;
import com.library.repository.UserRepository;
import com.library.repository.entity.EmailVerificationTokenEntity;
import com.library.repository.entity.UserEntity;
import com.library.service.AuthServiceImpl;
import com.library.service.EmailAlreadyExistsException;
import com.library.service.EmailNotVerifiedException;
import com.library.service.EmailService;
import com.library.service.InvalidCredentialsException;
import com.library.service.JwtService;
import com.library.service.ResendRateLimitedException;
import com.library.types.dto.LoginRequest;
import com.library.types.dto.LoginResponse;
import com.library.types.dto.RegisterRequest;
import com.library.types.dto.UserDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        VerificationConfig config = new VerificationConfig();
        config.setTokenExpiryMinutes(15);
        config.setResendCooldownSeconds(60);
        config.setBaseUrl("http://localhost:8080");

        authService = new AuthServiceImpl(
            userRepository,
            tokenRepository,
            passwordEncoder,
            jwtService,
            emailService,
            config,
            new SimpleMeterRegistry()
        );
    }

    @Test
    void register_validEmail_returnsUserDtoWithEmailVerifiedFalse() {
        RegisterRequest request = RegisterRequest.builder()
            .email("alice@example.com")
            .password("password123")
            .confirmPassword("password123")
            .build();

        UUID userId = UUID.randomUUID();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(UserEntity.class)))
            .thenAnswer(inv -> {
                UserEntity e = inv.getArgument(0);
                return UserEntity.builder()
                    .id(userId)
                    .email(e.getEmail())
                    .passwordHash(e.getPasswordHash())
                    .emailVerified(false)
                    .build();
            });
        when(tokenRepository.save(any(EmailVerificationTokenEntity.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        UserDto result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getId()).isNotNull();
        assertThat(result.isEmailVerified()).isFalse();
    }

    @Test
    void register_validEmail_sendsVerificationEmail() {
        RegisterRequest request = RegisterRequest.builder()
            .email("alice@example.com")
            .password("password123")
            .confirmPassword("password123")
            .build();

        UUID userId = UUID.randomUUID();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(UserEntity.class)))
            .thenReturn(UserEntity.builder()
                .id(userId)
                .email("alice@example.com")
                .passwordHash("$2a$12$hashed")
                .emailVerified(false)
                .build());
        when(tokenRepository.save(any(EmailVerificationTokenEntity.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        authService.register(request);

        verify(emailService).sendVerificationEmail(eq("alice@example.com"), anyString());
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExistsException() {
        RegisterRequest request = RegisterRequest.builder()
            .email("alice@example.com")
            .password("password123")
            .confirmPassword("password123")
            .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessageContaining("alice@example.com");
    }

    @Test
    void login_validCredentialsVerifiedUser_returnsLoginResponse() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
            .id(userId)
            .email("alice@example.com")
            .passwordHash("$2a$12$hashed")
            .emailVerified(true)
            .build();

        LoginRequest request = LoginRequest.builder()
            .email("alice@example.com")
            .password("password123")
            .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);
        when(jwtService.generate(eq(userId), eq("alice@example.com"))).thenReturn("jwt-token-123");

        LoginResponse result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getToken()).isEqualTo("jwt-token-123");
    }

    @Test
    void login_validCredentialsUnverifiedUser_throwsEmailNotVerifiedException() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
            .id(userId)
            .email("alice@example.com")
            .passwordHash("$2a$12$hashed")
            .emailVerified(false)
            .build();

        LoginRequest request = LoginRequest.builder()
            .email("alice@example.com")
            .password("password123")
            .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(EmailNotVerifiedException.class);
    }

    @Test
    void login_invalidEmail_throwsInvalidCredentialsException() {
        LoginRequest request = LoginRequest.builder()
            .email("nonexistent@example.com")
            .password("password123")
            .build();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_invalidPassword_throwsInvalidCredentialsException() {
        UserEntity user = UserEntity.builder()
            .id(UUID.randomUUID())
            .email("alice@example.com")
            .passwordHash("$2a$12$hashed")
            .emailVerified(true)
            .build();

        LoginRequest request = LoginRequest.builder()
            .email("alice@example.com")
            .password("wrongpassword")
            .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$12$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    void verifyEmail_validToken_setsEmailVerifiedTrue() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        EmailVerificationTokenEntity tokenEntity = EmailVerificationTokenEntity.builder()
            .id(tokenId)
            .userId(userId)
            .token("valid-token")
            .expiresAt(Instant.now().plusSeconds(3600))
            .createdAt(Instant.now())
            .build();

        UserEntity user = UserEntity.builder()
            .id(userId)
            .email("alice@example.com")
            .passwordHash("$2a$12$hashed")
            .emailVerified(false)
            .build();

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(tokenEntity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        doNothing().when(tokenRepository).deleteAllByUserId(userId);

        String result = authService.verifyEmail("valid-token");

        assertThat(result).isEqualTo("success");
        verify(userRepository).save(any(UserEntity.class));
        verify(tokenRepository).deleteAllByUserId(userId);
    }

    @Test
    void verifyEmail_expiredToken_returnsError() {
        UUID tokenId = UUID.randomUUID();

        EmailVerificationTokenEntity tokenEntity = EmailVerificationTokenEntity.builder()
            .id(tokenId)
            .userId(UUID.randomUUID())
            .token("expired-token")
            .expiresAt(Instant.now().minusSeconds(3600))
            .createdAt(Instant.now().minusSeconds(7200))
            .build();

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(tokenEntity));

        String result = authService.verifyEmail("expired-token");

        assertThat(result).isEqualTo("expired");
    }

    @Test
    void verifyEmail_invalidToken_returnsError() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        String result = authService.verifyEmail("invalid-token");

        assertThat(result).isEqualTo("invalid");
    }

    @Test
    void resendVerification_existingUnverifiedUser_sendsEmail() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
            .id(userId)
            .email("alice@example.com")
            .passwordHash("$2a$12$hashed")
            .emailVerified(false)
            .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(Optional.empty());
        when(tokenRepository.save(any(EmailVerificationTokenEntity.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(tokenRepository).deleteAllByUserId(userId);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        authService.resendVerification("alice@example.com");

        verify(emailService).sendVerificationEmail(eq("alice@example.com"), anyString());
    }

    @Test
    void resendVerification_alreadyVerifiedUser_doesNotSendEmail() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
            .id(userId)
            .email("alice@example.com")
            .passwordHash("$2a$12$hashed")
            .emailVerified(true)
            .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        authService.resendVerification("alice@example.com");

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void resendVerification_nonexistentEmail_doesNotThrow() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        authService.resendVerification("unknown@example.com");

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void resendVerification_withinCooldown_throwsException() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
            .id(userId)
            .email("alice@example.com")
            .passwordHash("$2a$12$hashed")
            .emailVerified(false)
            .build();

        // Token created 10 seconds ago — within 60s cooldown
        EmailVerificationTokenEntity recentToken = EmailVerificationTokenEntity.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .token("recent-token")
            .expiresAt(Instant.now().plusSeconds(3600))
            .createdAt(Instant.now().minusSeconds(10))
            .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(Optional.of(recentToken));

        assertThatThrownBy(() -> authService.resendVerification("alice@example.com"))
            .isInstanceOf(ResendRateLimitedException.class);
    }
}
