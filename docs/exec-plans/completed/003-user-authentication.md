# Execution Plan 003: User Authentication (F003)

**Feature**: F003 — User Authentication
**Status**: completed
**Product Spec**: [003-user-authentication.md](../../product-specs/003-user-authentication.md)
**Started**: 2026-03-12
**Completed**: 2026-03-12

## Goal

Users can create accounts and log in with username and password. Passwords are hashed with BCrypt and stored in the local PostgreSQL database. Login returns a JWT token for future authenticated requests.

## Acceptance Criteria

From `features.json`: All 12 criteria met.

## Implementation Steps

### Phase 1: Backend — Database & Auth API

- [x] Create `V2__create_users_table.sql`
- [x] Add Spring Security dependency for BCrypt
- [x] Create `types/dto/UserDto.java`, `RegisterRequest.java`, `LoginRequest.java`, `LoginResponse.java`
- [x] Create `repository/entity/UserEntity.java`, `UserRepository.java`
- [x] Create `config/SecurityConfig.java` — permit all, BCrypt strength 12
- [x] Create `service/AuthService.java`, `AuthServiceImpl.java`
- [x] Create `controller/AuthController.java`
- [x] Add `UsernameAlreadyExistsException`, `InvalidCredentialsException`; handle in GlobalExceptionHandler
- [x] Unit tests: `AuthServiceTest.java`
- [x] Integration tests: `AuthIntegrationTest.java`

### Phase 2: JWT Session Handling

- [x] Add jjwt dependencies to pom.xml
- [x] Add JWT config (app.jwt.secret, app.jwt.expiry-minutes)
- [x] Create `JwtService` interface, `JwtServiceImpl`
- [x] Integrate JWT into AuthServiceImpl login
- [x] Document client delivery: `Authorization: Bearer <token>`

### Phase 3: Frontend

- [x] Add session state: `st_user`, `st_token`
- [x] Add `register_user()`, `login_user()` API helpers
- [x] Create Account + Login expanders in header
- [x] Welcome + Logout when logged in

### Phase 4: API Reference & Verification

- [x] Add Auth section to docs/API_REFERENCE.md
- [x] Full test suite passed

## Technical Decisions

- **Session strategy**: JWT (stateless) — see design-docs/002-session-strategy.md
- **Client delivery**: `Authorization: Bearer <token>` header
- **Logout**: Client discards token (soft); no server-side revocation
- **JwtService interface**: Added for unit test mocking (Mockito/ByteBuddy compatibility with Java 23)
