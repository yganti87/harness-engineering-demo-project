# Execution Plan 007: Email-Based Authentication with Email Verification (F007)

**Feature**: F007 — Email-Based Authentication with Email Verification
**Status**: planned
**Product Spec**: [007-email-verification.md](../../product-specs/007-email-verification.md)
**Started**: 2026-03-18

## Goal

Replace username-based authentication with email-based authentication. Users must verify their email via a time-limited link before they can log in. Local SMTP via Mailpit; all SMTP config is environment-variable driven for production readiness. Database is stateless between container restarts.

## Acceptance Criteria

From `features.json` entry F007: 23 criteria.

## Logging & Observability Requirements

All auth-related actions must be logged with structured fields for observability. Use SLF4J Logger in each service/controller class.

### Log Levels by Operation

| Operation | Level | Message Pattern | Key Fields |
|-----------|-------|-----------------|------------|
| Registration success | INFO | `User registered, verification email sent` | `email` (masked), `userId` |
| Registration duplicate email | WARN | `Registration attempted with existing email` | `email` (masked) |
| Verification email sent | INFO | `Verification email sent` | `email` (masked), `tokenExpiresAt` |
| Verification email send failure | ERROR | `Failed to send verification email` | `email` (masked), `error` |
| Verification success | INFO | `Email verified successfully` | `userId` |
| Verification expired token | WARN | `Verification attempted with expired token` | `tokenId` |
| Verification invalid token | WARN | `Verification attempted with invalid token` | _(no PII)_ |
| Login success (verified) | INFO | `User logged in` | `userId` |
| Login failed (unverified) | WARN | `Login attempted by unverified user` | `email` (masked) |
| Login failed (bad credentials) | WARN | `Login failed: invalid credentials` | `email` (masked) |
| Resend verification sent | INFO | `Verification email resent` | `email` (masked) |
| Resend rate limited | WARN | `Resend verification rate limited` | `email` (masked), `cooldownSeconds` |

### Email Masking

Never log full email addresses. Use a masking utility: `a***@example.com` (first char + `***` + domain). Create `com.library.types.util.EmailMaskUtil` with a static `mask(String email)` method.

### Metrics (Prometheus counters via Micrometer)

| Metric | Type | Labels | Purpose |
|--------|------|--------|---------|
| `auth_registration_total` | Counter | `status=success\|duplicate_email\|error` | Track registration attempts |
| `auth_login_total` | Counter | `status=success\|unverified\|invalid_credentials` | Track login attempts |
| `auth_email_verification_total` | Counter | `status=success\|expired\|invalid` | Track verification attempts |
| `auth_verification_email_sent_total` | Counter | `status=success\|error` | Track email delivery |
| `auth_resend_verification_total` | Counter | `status=sent\|rate_limited\|no_action` | Track resend attempts |

Use `MeterRegistry` injected into `AuthServiceImpl` and `EmailServiceImpl`.

### Health Check

Add Mailpit SMTP connectivity to Spring Boot health checks via custom `MailHealthIndicator` in config layer — reports `DOWN` if SMTP is unreachable.

---

## Implementation Steps

### Phase 1: Infrastructure & Config

- [ ] **docker-compose.yml**: Add Mailpit service
  ```yaml
  mailpit:
    image: axllent/mailpit:v1.21
    container_name: library-mailpit
    restart: unless-stopped
    ports:
      - "${MAILPIT_UI_PORT:-8025}:8025"
      - "${MAILPIT_SMTP_PORT:-1025}:1025"
    networks:
      - library-network
  ```
- [ ] **docker-compose.yml**: Remove `postgres-data` named volume, switch db to tmpfs
  ```yaml
  db:
    tmpfs:
      - /var/lib/postgresql/data
    volumes:
      - ./database/init:/docker-entrypoint-initdb.d:ro
    # Remove: - postgres-data:/var/lib/postgresql/data
  ```
- [ ] **docker-compose.yml**: Add env vars to backend service
  ```
  MAIL_HOST=mailpit
  MAIL_PORT=1025
  VERIFICATION_BASE_URL=http://localhost:8080
  MAIL_FROM_ADDRESS=noreply@library.local
  ```
- [ ] **backend/pom.xml**: Add `spring-boot-starter-mail` dependency
- [ ] **backend/src/main/resources/application.yml**: Add mail + verification config
  ```yaml
  spring:
    mail:
      host: ${MAIL_HOST:localhost}
      port: ${MAIL_PORT:1025}
      username: ${MAIL_USERNAME:}
      password: ${MAIL_PASSWORD:}
      properties:
        mail.smtp.auth: ${MAIL_SMTP_AUTH:false}
        mail.smtp.starttls.enable: ${MAIL_STARTTLS_ENABLED:false}
  app:
    mail:
      from-address: ${MAIL_FROM_ADDRESS:noreply@library.local}
    verification:
      token-expiry-minutes: ${VERIFICATION_TOKEN_EXPIRY_MINUTES:15}
      resend-cooldown-seconds: 60
      base-url: ${VERIFICATION_BASE_URL:http://localhost:8080}
  ```

### Phase 2: Database Migration

- [ ] **V3__add_email_verification.sql**: Rename username→email, add email_verified, create email_verification_tokens table
  ```sql
  ALTER TABLE users RENAME COLUMN username TO email;
  ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(255);
  ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
  DROP INDEX IF EXISTS idx_users_username;
  CREATE INDEX idx_users_email ON users (email);
  ALTER TABLE users DROP CONSTRAINT users_username_key;
  ALTER TABLE users ADD CONSTRAINT users_email_key UNIQUE (email);

  CREATE TABLE email_verification_tokens (
      id         UUID         NOT NULL DEFAULT gen_random_uuid(),
      user_id    UUID         NOT NULL,
      token      VARCHAR(255) NOT NULL,
      expires_at TIMESTAMPTZ  NOT NULL,
      created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
      CONSTRAINT evt_pkey PRIMARY KEY (id),
      CONSTRAINT evt_token_key UNIQUE (token),
      CONSTRAINT evt_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  );
  CREATE INDEX idx_evt_token ON email_verification_tokens (token);
  CREATE INDEX idx_evt_user_id ON email_verification_tokens (user_id);
  ```

### Phase 3: Types Layer

- [ ] **RegisterRequest.java**: Replace `username` field with `email` (`@Email @NotBlank`). Remove username regex pattern. Keep password + confirmPassword unchanged.
- [ ] **LoginRequest.java**: Replace `username` field with `email` (`@Email @NotBlank`).
- [ ] **LoginResponse.java**: Replace `username` field with `email`.
- [ ] **UserDto.java**: Replace `username` field with `email`. Add `boolean emailVerified`.
- [ ] **ResendVerificationRequest.java** (new): Single `email` field (`@Email @NotBlank`).
- [ ] **MessageResponse.java** (new): Single `String message` field for generic API responses.
- [ ] **EmailMaskUtil.java** (new): Static `mask(String email)` method — returns `a***@example.com` format. Place in `com.library.types.util`.

### Phase 4: Repository Layer

- [ ] **UserEntity.java**: Rename `username` → `email` (column mapping `email`). Add `emailVerified` boolean field (column `email_verified`, default false).
- [ ] **UserRepository.java**: Replace `findByUsername(String)` → `findByEmail(String)`. Replace `existsByUsername(String)` → `existsByEmail(String)`.
- [ ] **EmailVerificationTokenEntity.java** (new): Entity for `email_verification_tokens` table. Fields: id (UUID), userId (UUID FK), token (String), expiresAt (Instant), createdAt (Instant).
- [ ] **EmailVerificationTokenRepository.java** (new): Methods:
  - `Optional<EmailVerificationTokenEntity> findByToken(String token)`
  - `void deleteAllByUserId(UUID userId)`
  - `Optional<EmailVerificationTokenEntity> findTopByUserIdOrderByCreatedAtDesc(UUID userId)`

### Phase 5: Config Layer

- [ ] **VerificationConfig.java** (new): `@ConfigurationProperties(prefix = "app.verification")`. Fields: `int tokenExpiryMinutes` (default 15), `int resendCooldownSeconds` (default 60), `String baseUrl`.
- [ ] **MailProperties.java** (new): `@ConfigurationProperties(prefix = "app.mail")`. Fields: `String fromAddress`.
- [ ] **MailHealthIndicator.java** (new): Custom health indicator that checks SMTP connectivity. Reports UP/DOWN in `/actuator/health`.

### Phase 6: Service Layer

- [ ] **EmailService.java** (new interface): Method `void sendVerificationEmail(String toEmail, String token)`.
- [ ] **EmailServiceImpl.java** (new): Implements EmailService. Uses `JavaMailSender` to send HTML email with verification link. Injects `MailProperties` for from-address and `VerificationConfig` for base URL. Logs INFO on success, ERROR on failure. Increments `auth_verification_email_sent_total` counter.
- [ ] **EmailNotVerifiedException.java** (new): Extends `RuntimeException`. Maps to HTTP 403.
- [ ] **EmailAlreadyExistsException.java** (new): Extends `RuntimeException`. Maps to HTTP 409.
- [ ] **Delete UsernameAlreadyExistsException.java** (replaced by EmailAlreadyExistsException).
- [ ] **AuthService.java**: Add methods `String verifyEmail(String token)`, `void resendVerification(String email)`. Update register/login signatures for email.
- [ ] **AuthServiceImpl.java**: Full rewrite:
  - `register()`: Validate email uniqueness → hash password → save UserEntity (emailVerified=false) → generate UUID token → save EmailVerificationTokenEntity (expires in configurable minutes) → send verification email → log INFO → increment counter → return UserDto.
  - `login()`: Find by email → verify password → check emailVerified (throw EmailNotVerifiedException if false, log WARN, increment counter) → generate JWT → log INFO → increment counter → return LoginResponse.
  - `verifyEmail(token)`: Find token → check expiry → set emailVerified=true → delete all tokens for user → log INFO → increment counter. Return "success" or "error" string for HTML rendering.
  - `resendVerification(email)`: Find user → if not found or already verified, return silently (log INFO) → check rate limit via most recent token's createdAt → generate new token → invalidate old tokens → send email → log INFO → increment counter.
  - Inject `MeterRegistry` for Prometheus counters.
- [ ] **JwtServiceImpl.java**: Change claim key from `"username"` to `"email"`.
- [ ] **JwtService.java**: Update method signature parameter name from `username` to `email` (if applicable).

### Phase 7: Controller Layer

- [ ] **AuthController.java**: Update register/login to use email-based DTOs. Add:
  - `GET /api/v1/auth/verify?token={token}` — `@GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)`. Returns `ResponseEntity<String>` with HTML content. Calls `authService.verifyEmail(token)`.
  - `POST /api/v1/auth/resend-verification` — accepts `@Valid @RequestBody ResendVerificationRequest`. Returns `ResponseEntity<ApiResponse<MessageResponse>>` with generic success message.
- [ ] **GlobalExceptionHandler.java**: Replace `UsernameAlreadyExistsException` → `EmailAlreadyExistsException` (409). Add `EmailNotVerifiedException` handler → 403 Forbidden.

### Phase 8: Frontend

- [ ] **frontend/app.py**:
  - **API helpers**: `register_user(email, password, confirm_password)` — change payload key to `email`. `login_user(email, password)` — change payload key to `email`. Handle 403 response in login (set `st_email_not_verified` state). New `resend_verification(email)` helper → POST to `/api/v1/auth/resend-verification`.
  - **Session state**: Add `st_pending_verification_email` (string or None), `st_email_not_verified` (bool).
  - **Create Account tab**: "Email" input (not "Username"). On success: set `st_pending_verification_email`, show "Check your email" view with email displayed, resend button, and "Go to Log In" button.
  - **Log In tab**: "Email" input (not "Username"). On 403: show "Email not verified" error with resend button.
  - **Catalog header**: `Welcome, {email}` (not username).
  - **Login response parsing**: Use `email` field from response (not `username`).

### Phase 9: Tests

#### Unit Tests (Mockito, no Spring context)

- [ ] **AuthServiceTest.java** (rewrite):
  - `register_validEmail_returnsUserDtoWithEmailVerifiedFalse`
  - `register_validEmail_sendsVerificationEmail`
  - `register_duplicateEmail_throwsEmailAlreadyExistsException`
  - `login_validCredentialsVerifiedUser_returnsLoginResponse`
  - `login_validCredentialsUnverifiedUser_throwsEmailNotVerifiedException`
  - `login_invalidEmail_throwsInvalidCredentialsException`
  - `login_invalidPassword_throwsInvalidCredentialsException`
  - `verifyEmail_validToken_setsEmailVerifiedTrue`
  - `verifyEmail_expiredToken_returnsError`
  - `verifyEmail_invalidToken_returnsError`
  - `resendVerification_existingUnverifiedUser_sendsEmail`
  - `resendVerification_alreadyVerifiedUser_doesNotSendEmail`
  - `resendVerification_nonexistentEmail_doesNotThrow`
  - `resendVerification_withinCooldown_throwsException`
- [ ] **EmailServiceTest.java** (new):
  - `sendVerificationEmail_sendsWithCorrectSubjectAndLink`
  - `sendVerificationEmail_usesConfiguredFromAddress`
- [ ] **EmailMaskUtilTest.java** (new):
  - `mask_standardEmail_masksLocalPart`
  - `mask_singleCharLocal_masksCorrectly`
  - `mask_nullOrEmpty_returnsPlaceholder`

#### Integration Tests (Testcontainers)

- [ ] **AuthIntegrationTest.java** (rewrite):
  - `register_validEmail_returns201`
  - `register_duplicateEmail_returns409`
  - `register_invalidEmailFormat_returns400`
  - `register_passwordMismatch_returns400`
  - `login_verifiedUser_returns200WithToken`
  - `login_unverifiedUser_returns403`
  - `login_invalidPassword_returns401`
  - `login_nonexistentEmail_returns401`
  - `verify_validToken_returns200Html`
  - `verify_expiredToken_returnsErrorHtml`
  - `verify_invalidToken_returnsErrorHtml`
  - `resendVerification_returns200`

  Note: Integration tests should mock `JavaMailSender` (or use `@TestConfiguration`) since Testcontainers won't have Mailpit.

#### Architecture & Style

- [ ] Verify `LayerDependencyTest` passes (all new classes in correct packages)
- [ ] Verify Checkstyle passes

#### Frontend Tests

- [ ] **frontend/tests/test_app.py**: Update all auth helper tests to use `email` instead of `username` in payloads and assertions.

### Phase 10: Scripts & Documentation

- [ ] **scripts/start.sh**: Add Mailpit URL to success banner: `Mailpit (email): http://localhost:8025`
- [ ] **docs/PLANS.md**: Add 007-email-verification to active plans table
- [ ] **features.json**: Already updated (status: planned → in_progress during implementation)

---

## Test Plan (Manual Verification)

```bash
# 1. Start fresh
docker compose down && docker compose up -d

# 2. Verify Mailpit is running
curl -s http://localhost:8025 | head -5

# 3. Register a new user
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password123","confirmPassword":"password123"}'
# Expect: 201 with emailVerified=false

# 4. Try to login before verification
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password123"}'
# Expect: 403 email not verified

# 5. Check Mailpit for verification email
curl -s http://localhost:8025/api/v1/messages | python3 -m json.tool
# Expect: email with verification link

# 6. Click verification link (extract token from email)
curl -s "http://localhost:8080/api/v1/auth/verify?token=<TOKEN>"
# Expect: HTML "Email Verified!" page

# 7. Login after verification
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password123"}'
# Expect: 200 with JWT token

# 8. Try duplicate registration
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password123","confirmPassword":"password123"}'
# Expect: 409

# 9. Resend verification
curl -s -X POST http://localhost:8080/api/v1/auth/resend-verification \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com"}'
# Expect: 200 with generic message

# 10. Verify DB statelessness
docker compose down && docker compose up -d
# Previous user should be gone — login should return 401

# 11. Check Prometheus metrics
curl -s http://localhost:8080/actuator/prometheus | grep auth_
# Expect: auth_registration_total, auth_login_total, etc.

# 12. Check health endpoint
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
# Expect: mail health indicator present
```

## Technical Decisions

- **Email delivery**: Mailpit for local dev, environment-variable driven SMTP for production
- **Verification token**: UUID v4, stored in DB, expires in 15 min (configurable)
- **DB statelessness**: tmpfs mount replaces named volume; Flyway re-runs all migrations on startup
- **HTML verification page**: GET endpoint returns HTML directly (exception to ApiResponse envelope) because the user clicks a link in their email browser
- **Email masking**: First char + `***` + domain in all log messages — never log full email
- **Metrics**: Micrometer counters for all auth operations, scrapped by existing Prometheus setup
- **Rate limiting**: Simple token-based (check most recent token's createdAt), not distributed — sufficient for single-instance dev app
