# Product Spec 007: Email-Based Authentication with Email Verification

**Feature ID**: F007
**Status**: planned

## User Story

As a library visitor, I want to register and log in with my email address, and verify my email via a time-limited link, so that my identity is tied to a real email and only verified users can access the catalog.

## Summary

Replace username-based authentication with email-based authentication. After registration, users receive a verification email containing a time-limited link (default 15 minutes). Users cannot log in until they click the verification link. A local SMTP server (Mailpit) provides email delivery in Docker dev environments, and all SMTP settings are environment-variable driven so any production SMTP provider (SendGrid, AWS SES, etc.) can be used without code changes. The database becomes stateless — no data persists across `docker compose down/up`.

## UX Design

### Create Account Tab (email-based)

```
┌──────────────────────────────────────────────┐
│  [  Log In  ]  |  [ Create Account ]          │
├──────────────────────────────────────────────┤
│  Create your account                          │
│                                               │
│  Email:    [____________________________]     │
│  Password: [____________________________]     │
│  Confirm:  [____________________________]     │
│                                               │
│  [         Create Account         ]           │
└──────────────────────────────────────────────┘
```

### Post-Registration — Verification Pending View

```
┌──────────────────────────────────────────────┐
│  Check your email                             │
│                                               │
│  We've sent a verification link to            │
│  alice@example.com                            │
│                                               │
│  Click the link in the email to activate      │
│  your account. The link expires in 15 min.    │
│                                               │
│  Didn't receive the email?                    │
│  [ Resend Verification Email ]                │
│                                               │
│  [ Go to Log In ]                             │
└──────────────────────────────────────────────┘
```

### Log In Tab (email-based)

```
┌──────────────────────────────────────────────┐
│  [  Log In  ]  |  [ Create Account ]          │
├──────────────────────────────────────────────┤
│  Welcome back                                 │
│                                               │
│  Email:    [____________________________]     │
│  Password: [____________________________]     │
│                                               │
│  [            Log In              ]           │
└──────────────────────────────────────────────┘
```

### Login Error — Unverified Account

```
┌──────────────────────────────────────────────┐
│  [!] Your email has not been verified yet.    │
│      Please check your inbox or resend the    │
│      verification email below.                │
│                                               │
│  [ Resend Verification Email ]                │
└──────────────────────────────────────────────┘
```

### Verification Success (HTML page served by backend)

```
┌──────────────────────────────────────────────┐
│  Email Verified!                              │
│                                               │
│  Your account is now active.                  │
│  You can close this tab and log in.           │
└──────────────────────────────────────────────┘
```

### Verification Failure (expired/invalid token)

```
┌──────────────────────────────────────────────┐
│  Verification Failed                          │
│                                               │
│  This link has expired or is invalid.         │
│  Please request a new verification email.     │
└──────────────────────────────────────────────┘
```

## API Contract

### Register (modified)

`POST /api/v1/auth/register`

**Request Body**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `email` | string | Yes | Valid email format (@Email) |
| `password` | string | Yes | Min 8 chars |
| `confirmPassword` | string | Yes | Must match `password` |

**Success** (201): `ApiResponse<UserDto>` — id, email, emailVerified=false

**Side effect**: Sends verification email via SMTP. Creates verification token in DB.

**Errors**
- 400: Validation failure (invalid email format, password too short, mismatch)
- 409: Email already registered

---

### Login (modified)

`POST /api/v1/auth/login`

**Request Body**

| Field | Type | Required |
|-------|------|----------|
| `email` | string | Yes |
| `password` | string | Yes |

**Success** (200): `ApiResponse<LoginResponse>` — userId, email, token (JWT)

**Errors**
- 400: Validation failure (missing fields)
- 401: Invalid email or password
- 403: Email not verified

---

### Verify Email (new)

`GET /api/v1/auth/verify?token={token}`

Browser-navigable endpoint. Returns **HTML** (not JSON) — exception to ApiResponse envelope.

- Valid token: sets `email_verified=true`, deletes tokens, returns success HTML page
- Expired token: returns error HTML page
- Invalid/missing token: returns error HTML page

---

### Resend Verification (new)

`POST /api/v1/auth/resend-verification`

**Request Body**

| Field | Type | Required |
|-------|------|----------|
| `email` | string | Yes |

**Success** (200): `ApiResponse<MessageResponse>` — always returns same message regardless of whether email exists (prevents enumeration)

**Rate limit**: 1 resend per email per 60 seconds (enforced via token created_at check)

---

## Data Model

### users table (modified via V3 migration)

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK, default gen_random_uuid() |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL (was `username`) |
| `password_hash` | VARCHAR(255) | NOT NULL |
| `email_verified` | BOOLEAN | NOT NULL, DEFAULT FALSE (new) |
| `created_at` | TIMESTAMPTZ | NOT NULL, default now() |
| `updated_at` | TIMESTAMPTZ | NOT NULL, default now() |

### email_verification_tokens table (new)

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK, default gen_random_uuid() |
| `user_id` | UUID | FK → users(id) ON DELETE CASCADE |
| `token` | VARCHAR(255) | UNIQUE, NOT NULL |
| `expires_at` | TIMESTAMPTZ | NOT NULL |
| `created_at` | TIMESTAMPTZ | NOT NULL, default now() |

## Security Requirements

- **Password hashing**: BCrypt strength >= 12 (unchanged)
- **Never log**: passwords, password hashes, tokens, or email verification tokens at any log level
- **Verification tokens**: UUID v4, single-use, expire in 15 minutes (configurable)
- **Email enumeration prevention**: `/resend-verification` always returns same 200 response
- **Resend rate limiting**: 1 per email per 60 seconds
- **Email validation**: `@Email` jakarta.validation annotation on all email fields
- **SMTP config**: Environment-variable driven — Mailpit for dev, real SMTP for production (no code changes)

## Infrastructure Changes

### Stateless Database
- Remove `postgres-data` named Docker volume
- Use `tmpfs` mount for PostgreSQL data dir
- Flyway re-runs all migrations on each startup (seed data from V1 re-inserted)

### Mailpit (local SMTP)
- Docker service: `axllent/mailpit` on ports 8025 (UI) and 1025 (SMTP)
- Web UI at `http://localhost:8025` for viewing sent emails

### Production-Ready Mail Config
- Environment variables: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS_ENABLED`, `MAIL_FROM_ADDRESS`
- Local dev defaults: Mailpit (no auth, no TLS)
- Production: set env vars to point at any standard SMTP provider

## Acceptance Criteria

See `features.json` entry F007.

## Out of Scope

- Password reset / forgot password
- OAuth / social login
- Email change after registration
- CAPTCHA on registration
- Account lockout after failed login attempts
- Two-factor authentication
- Admin panel for user management
- Email templates with branding/styling (plain HTML is fine)
