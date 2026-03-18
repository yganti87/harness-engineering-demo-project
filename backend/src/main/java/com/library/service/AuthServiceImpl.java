package com.library.service;

import com.library.config.VerificationConfig;
import com.library.repository.EmailVerificationTokenRepository;
import com.library.repository.UserRepository;
import com.library.repository.entity.EmailVerificationTokenEntity;
import com.library.repository.entity.UserEntity;
import com.library.types.dto.LoginRequest;
import com.library.types.dto.LoginResponse;
import com.library.types.dto.RegisterRequest;
import com.library.types.dto.UserDto;
import com.library.types.util.EmailMaskUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService}.
 *
 * <p>Handles email-based registration with verification, login (verified users only),
 * email verification via token, and resend-verification with rate limiting.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final VerificationConfig verificationConfig;
    private final MeterRegistry meterRegistry;

    // Registration counters
    private Counter regSuccessCounter;
    private Counter regDuplicateCounter;
    private Counter regErrorCounter;

    // Login counters
    private Counter loginSuccessCounter;
    private Counter loginUnverifiedCounter;
    private Counter loginInvalidCounter;

    // Verification counters
    private Counter verifySuccessCounter;
    private Counter verifyExpiredCounter;
    private Counter verifyInvalidCounter;

    // Resend counters
    private Counter resendSentCounter;
    private Counter resendRateLimitedCounter;
    private Counter resendNoActionCounter;

    @PostConstruct
    void initMetrics() {
        regSuccessCounter = Counter.builder("auth_registration_total")
            .tag("status", "success").register(meterRegistry);
        regDuplicateCounter = Counter.builder("auth_registration_total")
            .tag("status", "duplicate_email").register(meterRegistry);
        regErrorCounter = Counter.builder("auth_registration_total")
            .tag("status", "error").register(meterRegistry);

        loginSuccessCounter = Counter.builder("auth_login_total")
            .tag("status", "success").register(meterRegistry);
        loginUnverifiedCounter = Counter.builder("auth_login_total")
            .tag("status", "unverified").register(meterRegistry);
        loginInvalidCounter = Counter.builder("auth_login_total")
            .tag("status", "invalid_credentials").register(meterRegistry);

        verifySuccessCounter = Counter.builder("auth_email_verification_total")
            .tag("status", "success").register(meterRegistry);
        verifyExpiredCounter = Counter.builder("auth_email_verification_total")
            .tag("status", "expired").register(meterRegistry);
        verifyInvalidCounter = Counter.builder("auth_email_verification_total")
            .tag("status", "invalid").register(meterRegistry);

        resendSentCounter = Counter.builder("auth_resend_verification_total")
            .tag("status", "sent").register(meterRegistry);
        resendRateLimitedCounter = Counter.builder("auth_resend_verification_total")
            .tag("status", "rate_limited").register(meterRegistry);
        resendNoActionCounter = Counter.builder("auth_resend_verification_total")
            .tag("status", "no_action").register(meterRegistry);
    }

    @Override
    @Transactional
    public UserDto register(RegisterRequest request) {
        String maskedEmail = EmailMaskUtil.mask(request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempted with existing email email='{}'", maskedEmail);
            regDuplicateCounter.increment();
            throw new EmailAlreadyExistsException(
                "Email already registered: " + request.getEmail());
        }

        try {
            String passwordHash = passwordEncoder.encode(request.getPassword());

            UserEntity entity = UserEntity.builder()
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .emailVerified(false)
                .build();

            UserEntity saved = userRepository.save(entity);

            String token = UUID.randomUUID().toString();
            Instant expiresAt = Instant.now()
                .plus(verificationConfig.getTokenExpiryMinutes(), ChronoUnit.MINUTES);

            EmailVerificationTokenEntity tokenEntity = EmailVerificationTokenEntity.builder()
                .userId(saved.getId())
                .token(token)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .build();
            tokenRepository.save(tokenEntity);

            emailService.sendVerificationEmail(saved.getEmail(), token);

            regSuccessCounter.increment();
            log.info("User registered, verification email sent email='{}' userId='{}'",
                maskedEmail, saved.getId());

            return toDto(saved);
        } catch (EmailAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            regErrorCounter.increment();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String maskedEmail = EmailMaskUtil.mask(request.getEmail());

        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                log.warn("Login failed: invalid credentials email='{}'", maskedEmail);
                loginInvalidCounter.increment();
                return new InvalidCredentialsException("Invalid email or password");
            });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid credentials email='{}'", maskedEmail);
            loginInvalidCounter.increment();
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            log.warn("Login attempted by unverified user email='{}'", maskedEmail);
            loginUnverifiedCounter.increment();
            throw new EmailNotVerifiedException(
                "Email not verified. Please check your inbox for the verification email.");
        }

        String token = jwtService.generate(user.getId(), user.getEmail());
        loginSuccessCounter.increment();
        log.info("User logged in userId='{}'", user.getId());

        return LoginResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .token(token)
            .build();
    }

    @Override
    @Transactional
    public String verifyEmail(String token) {
        Optional<EmailVerificationTokenEntity> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("Verification attempted with invalid token");
            verifyInvalidCounter.increment();
            return "invalid";
        }

        EmailVerificationTokenEntity tokenEntity = tokenOpt.get();

        if (Instant.now().isAfter(tokenEntity.getExpiresAt())) {
            log.warn("Verification attempted with expired token tokenId='{}'",
                tokenEntity.getId());
            verifyExpiredCounter.increment();
            return "expired";
        }

        UUID userId = tokenEntity.getUserId();
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("Verification attempted with invalid token");
            verifyInvalidCounter.increment();
            return "invalid";
        }

        UserEntity user = userOpt.get();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteAllByUserId(userId);

        verifySuccessCounter.increment();
        log.info("Email verified successfully userId='{}'", userId);

        return "success";
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        String maskedEmail = EmailMaskUtil.mask(email);

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || userOpt.get().isEmailVerified()) {
            resendNoActionCounter.increment();
            log.info("Resend verification no action email='{}'", maskedEmail);
            return;
        }

        UserEntity user = userOpt.get();
        Optional<EmailVerificationTokenEntity> latestToken =
            tokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());

        if (latestToken.isPresent()) {
            Instant cooldownEnd = latestToken.get().getCreatedAt()
                .plus(verificationConfig.getResendCooldownSeconds(), ChronoUnit.SECONDS);
            if (Instant.now().isBefore(cooldownEnd)) {
                long remaining = ChronoUnit.SECONDS.between(Instant.now(), cooldownEnd);
                log.warn("Resend verification rate limited email='{}' cooldownSeconds='{}'",
                    maskedEmail, remaining);
                resendRateLimitedCounter.increment();
                throw new ResendRateLimitedException(
                    "Please wait before requesting another verification email.");
            }
        }

        tokenRepository.deleteAllByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now()
            .plus(verificationConfig.getTokenExpiryMinutes(), ChronoUnit.MINUTES);

        EmailVerificationTokenEntity tokenEntity = EmailVerificationTokenEntity.builder()
            .userId(user.getId())
            .token(token)
            .expiresAt(expiresAt)
            .createdAt(Instant.now())
            .build();
        tokenRepository.save(tokenEntity);

        emailService.sendVerificationEmail(email, token);

        resendSentCounter.increment();
        log.info("Verification email resent email='{}'", maskedEmail);
    }

    private UserDto toDto(UserEntity entity) {
        return UserDto.builder()
            .id(entity.getId())
            .email(entity.getEmail())
            .emailVerified(entity.isEmailVerified())
            .build();
    }
}
