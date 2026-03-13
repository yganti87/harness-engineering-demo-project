# Product Spec 003: User Authentication

**Feature ID**: F003
**Status**: completed

## User Story

As a library visitor, I want to create an account and log in with a username and password so that I can access personalized features (e.g., borrowing, reading history) and have my identity persisted across sessions.

## Summary

Users register with a unique username and password. Passwords are hashed (BCrypt, one-way) and stored in the local PostgreSQL database — never stored in plain text or reversibly encrypted. Login returns a session identifier (or token) for authenticated requests. Usernames must be unique and validated at registration.

## UX Design

```
┌─────────────────────────────────────────────────────────────────┐
│ Library Catalog                                                  │
│ Search our collection.                                           │
├─────────────────────────────────────────────────────────────────┤
│ [Search...] [All Genres ▾] [Search]  │  [ Create Account ]  [ Login ] │
└─────────────────────────────────────────────────────────────────┘

--- Create Account Modal / Panel ---
┌─────────────────────────────────────────────────────────────────┐
│ Create Account                                                    │
├─────────────────────────────────────────────────────────────────┤
│ Username:  [________________]   (3–50 chars, alphanumeric + _)   │
│ Password:  [________________]   (min 8 chars)                    │
│ Confirm:   [________________]                                   │
│                                                                  │
│                              [Cancel]  [Create Account]          │
└─────────────────────────────────────────────────────────────────┘

--- Login Modal / Panel ---
┌─────────────────────────────────────────────────────────────────┐
│ Log In                                                            │
├─────────────────────────────────────────────────────────────────┤
│ Username:  [________________]                                    │
│ Password:  [________________]                                    │
│                                                                  │
│                              [Cancel]  [Log In]                  │
└─────────────────────────────────────────────────────────────────┘

--- After Login ---
│ [Search...] [All Genres ▾] [Search]  │  Welcome, alice  [ Logout ] │
```

## API Contract

### Register

`POST /api/v1/auth/register`

**Request Body**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `username` | string | Yes | 3–50 chars, alphanumeric + underscore only |
| `password` | string | Yes | Min 8 chars |
| `confirmPassword` | string | Yes | Must match `password` |

**Success** (201): `ApiResponse<UserDto>` — user id, username (no password)

**Errors**
- 400: Validation failure (invalid format, password too short, mismatch)
- 409: Username already taken

---

### Login

`POST /api/v1/auth/login`

**Request Body**

| Field | Type | Required |
|-------|------|----------|
| `username` | string | Yes |
| `password` | string | Yes |

**Success** (200): `ApiResponse<LoginResponse>` — contains user id, username, and session token (or session id for cookie-based auth)

**Errors**
- 400: Validation failure (missing fields)
- 401: Invalid username or password

---

### Logout (optional for Phase 1)

`POST /api/v1/auth/logout`

**Success** (200): Invalidate session/token.

See [API_REFERENCE.md](../API_REFERENCE.md) for full spec after implementation.

## Data Model

### users table (Flyway migration)

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK, default gen_random_uuid() |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL |
| `password_hash` | VARCHAR(255) | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL, default now() |
| `updated_at` | TIMESTAMP | NOT NULL, default now() |

- Passwords are stored as BCrypt hashes (strength ≥ 12). Never store plain-text passwords.
- Index on `username` (unique) for fast login lookups.

## Acceptance Criteria

See `features.json` entry F003.

## Security Requirements

- **Password hashing**: BCrypt (Spring Security `BCryptPasswordEncoder` or equivalent), strength ≥ 12
- **Never log**: passwords, password hashes, or tokens at any log level
- **Session handling**: Use HTTP-only cookie or short-lived JWT; implementation choice documented
- **Input validation**: All DTOs validated via `jakarta.validation` before processing

## Out of Scope (this feature)

- Password reset / forgot password
- OAuth / social login
- Email verification
- Role-based access control (RBAC) — all logged-in users have same permissions
- Requiring auth for book search — search remains anonymous
